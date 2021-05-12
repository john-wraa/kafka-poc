package no.eksempel.kafka.connector.sources.github;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.apache.kafka.connect.errors.ConnectException;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class GitHubApiHttpClient {
    private static final Logger log = LoggerFactory.getLogger(GitHubApiHttpClient.class);

    // for efficient http requests
    @SuppressWarnings("FieldCanBeLocal")
    private Integer xRateLimit = 9999;
    private Integer xRateRemaining = 9999;
    private long xRateReset = Instant.MAX.getEpochSecond();

    GitHubSourceConnectorConfig config;

    public GitHubApiHttpClient(GitHubSourceConnectorConfig config){
        this.config = config;
    }

    protected JSONArray getNextIssues(Integer page, Instant since) throws InterruptedException {

        HttpResponse<JsonNode> jsonResponse;
        try {
            jsonResponse = getNextIssuesAPI(page, since);

            // deal with headers in any case
            var headers = jsonResponse.getHeaders();
            xRateLimit = Integer.valueOf(headers.getFirst("X-RateLimit-Limit"));
            xRateRemaining = Integer.valueOf(headers.getFirst("X-RateLimit-Remaining"));
            xRateReset = Integer.parseInt(headers.getFirst("X-RateLimit-Reset"));
            switch (jsonResponse.getStatus()){
                case 200:
                    return jsonResponse.getBody().getArray();
                case 401:
                    throw new ConnectException("Bad GitHub credentials provided, please edit your config");
                case 403:
                    // we have issued too many requests.
                    log.info(jsonResponse.getBody().getObject().getString("message"));
                    log.info("Your rate limit is {}", xRateLimit);
                    log.info("Your remaining calls is {}", xRateRemaining);
                    log.info("The limit will reset at {}",
                            LocalDateTime.ofInstant(Instant.ofEpochSecond(xRateReset), ZoneOffset.UTC));
                    long sleepTime = xRateReset - Instant.now().getEpochSecond();
                    log.info("Sleeping for {} seconds", sleepTime );
                    if (sleepTime<=0) {
                        sleepTime = 300;
                        log.info("Overriding sleep to {} seconds", sleepTime );
                    }
                    Thread.sleep(1000 * sleepTime);
                    return getNextIssues(page, since);
                default:
                    if (log.isErrorEnabled()) {
                        log.error(constructUrl(page, since));
                        log.error(String.valueOf(jsonResponse.getStatus()));
                        log.error(jsonResponse.getBody().toString());
                        log.error(jsonResponse.getHeaders().toString());
                        log.error("Unknown error: Sleeping 5 seconds before re-trying");

                    }
                    Thread.sleep(5000L);
                    return getNextIssues(page, since);
            }
        } catch (UnirestException e) {
            e.printStackTrace();
            Thread.sleep(5000L);
            return new JSONArray();
        }
    }

    protected HttpResponse<JsonNode> getNextIssuesAPI(Integer page, Instant since) throws UnirestException {
        GetRequest unirest = Unirest.get(constructUrl(page, since));
        if (!config.getAuthUsername().isEmpty() && !config.getAuthPassword().isEmpty() ){
            unirest = unirest.basicAuth(config.getAuthUsername(), config.getAuthPassword());
        }
        log.debug("GET {}", unirest.getUrl());
        return unirest.asJson();
    }

    protected String constructUrl(Integer page, Instant since){
        return String.format(
                "https://api.github.com/repos/%s/%s/issues?page=%s&per_page=%s&since=%s&state=all&direction=asc&sort=updated",
                config.getOwnerConfig(),
                config.getRepoConfig(),
                page,
                config.getBatchSize(),
                since.toString());
    }

    public void sleep() throws InterruptedException {
        long sleepTime = (long) Math.ceil(
                (double) (xRateReset - Instant.now().getEpochSecond()) / xRateRemaining);
        log.debug("Sleeping for {} seconds", sleepTime );
        Thread.sleep(1000 * sleepTime);
    }

    public void sleepIfNeed() throws InterruptedException {
        // Sleep if needed
        if (xRateRemaining <= 10 && xRateRemaining > 0) {
            log.info("Approaching limit soon, you have {} requests left", xRateRemaining);
            sleep();
        }
    }
}

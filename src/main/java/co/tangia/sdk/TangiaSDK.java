package co.tangia.sdk;

import com.mojang.logging.LogUtils;
import dev.failsafe.RetryPolicy;
import dev.failsafe.retrofit.FailsafeCall;
import org.slf4j.Logger;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;

public class TangiaSDK {
    public static final String PROD_URL = "https://api.tangia.co/";
    public static final String STAGING_URL = "https://tangia.staging.ultimatearcade.io/";

    private String sessionKey;
    private EventPoller eventPoller = new EventPoller();
    private final String gameID;
    private final String gameVersion;
    private final SynchronousQueue<InteractionEvent> events = new SynchronousQueue<>();
    private final Set<String> handledEventIds = new HashSet<>();
    private final TangiaApi api;

    private static final Logger LOGGER = LogUtils.getLogger();

    public TangiaSDK(String gameID, String gameVersion) {
        this(gameID, gameVersion, PROD_URL);
    }

    public TangiaSDK(String gameID, String gameVersion, String baseUrl) {
        this.gameID = gameID;
        this.gameVersion = gameVersion;
        this.api = createApi(baseUrl);
    }

    public void login(String creatorCode) throws IOException, InvalidLoginException {
        var call = api.login(new GameLoginReq(gameID, creatorCode));
        var res = execWithRetries(call);
        if (!res.isSuccessful() || res.body() == null)
            throw new InvalidLoginException();
        this.sessionKey = res.body().SessionID;
    }

    public void startEventPolling() {
        eventPoller.start();
    }

    public void stopEventPolling() {
        eventPoller.stopPolling();
        eventPoller = new EventPoller();
    }

    public InteractionEvent popEventQueue() {
        return events.poll();
    }

    public void ackEvents(EventResult[] results) throws IOException, InvalidRequestException {
        var call = api.ackEvents(this.sessionKey, new AckInteractionEventsReq(results));
        Response<Void> res = execWithRetries(call);
        if (!res.isSuccessful())
            throw new InvalidRequestException();
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    private <T> Response<T> execWithRetries(Call<T> call) throws IOException {
        RetryPolicy<Response<T>> retryPolicy = RetryPolicy.ofDefaults();
        var failsafeCall = FailsafeCall.with(retryPolicy).compose(call);
        return failsafeCall.execute();
    }

    private static TangiaApi createApi(String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .build();

        return retrofit.create(TangiaApi.class);
    }

    private class EventPoller extends Thread {
        private boolean stopped = false;

        @Override
        public void run() {
            super.run();
            try {
                while (!stopped) {
                    pollEvents();
                }
            } catch (InterruptedException ex) {
                LOGGER.warn("got interrupted, will stop event polling");
            }
            var stopCall = api.notifyStopPlaying(sessionKey);
            try {
                Response<Void> stopResp = execWithRetries(stopCall);
                if (!stopResp.isSuccessful())
                    LOGGER.warn("couldn't notify stop playing");
            } catch (IOException e) {
                LOGGER.warn("couldn't notify stop playing: " + e.getMessage());
            }
        }

        private void pollEvents() throws InterruptedException {
            var eventsCall = api.pollEvents(sessionKey, new InteractionEventsReq(gameVersion));
            Response<InteractionEventsResp> eventsResp = null;
            try {
                eventsResp = execWithRetries(eventsCall);
            } catch (IOException e) {
                LOGGER.warn("error when polling events: " + e.getMessage());
            }
            if (eventsResp == null || eventsResp.body() == null || !eventsResp.isSuccessful()) {
                LOGGER.warn("couldn't get events");
                Thread.sleep(100);
                return;
            }
            for (InteractionEvent e : eventsResp.body().Events) {
                // we'll receive events until they get ack'ed/rejected
                if (handledEventIds.contains(e.EventID))
                    continue;
                handledEventIds.add(e.EventID);
                events.put(e);
            }
        }

        public void stopPolling() {
            this.stopped = true;
        }
    }
}

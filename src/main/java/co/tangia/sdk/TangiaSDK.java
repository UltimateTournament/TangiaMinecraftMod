package co.tangia.sdk;

import com.mojang.logging.LogUtils;
import dev.failsafe.RetryPolicy;
import dev.failsafe.retrofit.FailsafeCall;
import org.slf4j.Logger;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public class TangiaSDK {
    public static final String PROD_URL = "https://api.tangia.co/";
    public static final String STAGING_URL = "https://tangia.staging.ultimatearcade.io/";

    private String sessionKey;
    private EventPoller eventPoller = new EventPoller();
    private final String versionInfo;
    private final ArrayBlockingQueue<InteractionEvent> eventQueue = new ArrayBlockingQueue<>(100);
    private final ArrayBlockingQueue<EventResult> eventAckQueue = new ArrayBlockingQueue<>(100);
    private final Set<String> handledEventIds = new HashSet<>();
    private final TangiaApi api;

    private static final Logger LOGGER = LogUtils.getLogger();

    public TangiaSDK(String versionInfo) {
        this(versionInfo, PROD_URL);
    }

    public TangiaSDK(String versionInfo, String baseUrl) {
        this.versionInfo = versionInfo;
        this.api = createApi(baseUrl);
    }

    public void login(String creatorCode) throws IOException, InvalidLoginException {
        var call = api.login(new IntegrationLoginReq(versionInfo, creatorCode));
        var res = execWithRetries(call);
        if (!res.isSuccessful() || res.body() == null)
            throw new InvalidLoginException(res.toString());
        this.sessionKey = res.body().AccountKey;
    }

    public void logout() {
        var call = api.logout(sessionKey);
        Response<Void> res;
        try {
            res = execWithRetries(call);
            if (!res.isSuccessful()) {
                LOGGER.warn("logout failed: code {}", res.code());
            }
        } catch (IOException e) {
            LOGGER.warn("logout failed", e);
        }
    }

    public void startEventPolling() {
        eventPoller.start();
    }

    public void stopEventPolling() {
        eventPoller.stopPolling();
        eventPoller = new EventPoller();
    }

    public InteractionEvent popEventQueue() {
        return eventQueue.poll();
    }

    public void ackEvent(String eventID) throws IOException, InvalidRequestException {
        var call = api.ackEvent(this.sessionKey, eventID);
        Response<Void> res = execWithRetries(call);
        if (!res.isSuccessful())
            throw new InvalidRequestException();
    }

    public void nackEvent(String eventID) throws IOException, InvalidRequestException {
        var call = api.nackEvent(this.sessionKey, eventID);
        Response<Void> res = execWithRetries(call);
        if (!res.isSuccessful())
            throw new InvalidRequestException();
    }

    public void ackEventAsync(EventResult e) {
        if (!eventAckQueue.offer(e)) {
            LOGGER.warn("ack-queue is full!");
        }
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
                .addConverterFactory(GsonConverterFactory.create())
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
        }

        private void pollEvents() throws InterruptedException {
            while (true) {
                var event = eventAckQueue.poll();
                if (event == null)
                    break;
                try {
                    if (event.Executed) {
                        ackEvent(event.EventID);
                    } else {
                        nackEvent(event.EventID);
                    }
                } catch (Exception e) {
                    LOGGER.warn("couldn't ack events: " + e);
                }
            }
            var eventsCall = api.pollEvents(sessionKey);
            Response<InteractionEventsResp> eventsResp = null;
            try {
                eventsResp = execWithRetries(eventsCall);
            } catch (IOException e) {
                LOGGER.warn("error when polling events: " + e.getMessage());
            }
            if (eventsResp == null || !eventsResp.isSuccessful()) {
                LOGGER.warn("couldn't get events");
                Thread.sleep(200);
                return;
            }
            var body = eventsResp.body();
            if (body == null || body.ActionExecutions == null || body.ActionExecutions.length == 0) {
                LOGGER.debug("no events");
                Thread.sleep(50);
                return;
            }
            for (var ae : body.ActionExecutions) {
                // we'll receive events until they get ack'ed/rejected
                if (handledEventIds.contains(ae.Body.EventID))
                    continue;
                handledEventIds.add(ae.Body.EventID);
                eventQueue.put(ae.Body);
            }
        }

        public void stopPolling() {
            this.stopped = true;
        }
    }
}

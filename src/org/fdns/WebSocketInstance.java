package org.fdns;

import com.google.gson.Gson;
import org.fdns.requests.*;

import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WebSocketInstance {

    Random random = new Random();
    Gson json = new Gson();
    ArrayList<String> domains = new ArrayList<>();
    Map<String, Host> hosts = new HashMap<>();
    Map<Integer, RequestBody> requestBodies = new LinkedHashMap<>();
    Long CONNECTION_TIMEOUT = 10000L; // milliseconds

    String selfIp = "localhost:9090";

    void send(String domain, Request request) throws ConnectException {
        WebSocketNetwork.send(domain, json.toJson(request));
    }

    void onMessage(String data) {

    }

    public void sendRequest(String domain, String data, Runnable success, Runnable error) {
        removeExpiredRequests();
        Integer requestId = random.nextInt();
        RequestBody requestBody = new RequestBody(requestId, new Date().getTime(), data, success, error);
        requestBodies.put(requestId, requestBody);
        sendProxyPathRequest(domain);
    }

    private void removeExpiredRequests() {
        Long expireTime = new Date().getTime() - CONNECTION_TIMEOUT;
        ArrayList<RequestBody> expiredRequests = new ArrayList<>();
        for (RequestBody requestBody : requestBodies.values())
            if (requestBody.startTime < expireTime)
                expiredRequests.add(requestBody);
        for (RequestBody requestBody : expiredRequests)
            onError(requestBody.requestId);
    }

    public void onReceive(String data) {
        Request request = json.fromJson(data, Request.class);
        String requestType = request.requestType;
        if (requestType.equals(ProxyPathRequest.class.getSimpleName())) {
            onProxyPathRequest(json.fromJson(data, ProxyPathRequest.class));
        } else if (requestType.equals(ProxyPathBackRequest.class.getSimpleName())) {
            onProxyPathBackRequest(json.fromJson(data, ProxyPathRequest.class));
        } else if (requestType.equals(ProxyDataRequest.class.getSimpleName())) {
            onProxyDataRequest(json.fromJson(data, ProxyDataRequest.class));
        } else if (requestType.equals(RegistrationRequest.class.getSimpleName())) {
            onRegistrationRequest(json.fromJson(data, RegistrationRequest.class));
        }
    }

    private void onError(Integer requestId) {
        RequestBody requestBody = requestBodies.get(requestId);
        if (requestBody != null && requestBody.error != null)
            requestBody.error.run();
        requestBodies.remove(requestId);
    }

    List<String> findSimilarDomains(String domain) {
        // TODO add limit of similar results
        int difference = Integer.MAX_VALUE;
        List<String> similarDomains = new ArrayList<>();
        for (String currentDomain : domains) {
            int currentDifference = StringComparator.compare(domain, currentDomain);
            if (currentDifference < difference) {
                difference = currentDifference;
                similarDomains.clear();
                similarDomains.add(currentDomain);
            } else if (currentDifference == difference) {
                similarDomains.add(currentDomain);
            }
        }
        if (difference == 0 && similarDomains.indexOf(domain) != -1) {
            similarDomains.clear();
            similarDomains.add(domain);
        }
        return similarDomains;
    }

    void filterPreviousDomains(List<String> similarDomains, List<String> domains) {
        // TODO develop
    }

    String findFastestDomain(List<String> domains) {
        return domains.get(0); // TODO develop
    }

    void sendProxyPathRequest(String domain) {
        onProxyPathRequest(new ProxyPathRequest(domain));
    }

    private void onProxyPathRequest(ProxyPathRequest request) {
        List<String> similarDomains = findSimilarDomains(request.findDomain);
        request.trace.add(selfIp);
        if (similarDomains.size() != 0) {
            filterPreviousDomains(similarDomains, request.trace);
            while (true) {
                String target = findFastestDomain(similarDomains);
                if (target == null)
                    break;
                try {
                    send(target, request);
                    break;
                } catch (ConnectException e) {
                    similarDomains.remove(target);
                }
            }
        }
        if (similarDomains.size() == 0) {
            request.requestType = ProxyPathBackRequest.class.getSimpleName();
            onProxyPathBackRequest(request);
        }
    }

    private void onProxyPathBackRequest(ProxyPathRequest request) {
        request.backtrace.add(selfIp);
        for (int i = 0; i < request.trace.size(); i++) {
            String traceIp = request.trace.get(i);
            if (traceIp.equals(selfIp)) {
                if (i == 0) {
                    onProxyPathFinish(request);
                    break;
                } else {
                    // trace items are disconnect
                    break;
                }
            } else {
                try {
                    send(traceIp, request);
                    break;
                } catch (ConnectException ignore) {
                }
            }
        }
    }

    private void onProxyPathFinish(ProxyPathRequest pathRequest) {
        RequestBody requestBody = requestBodies.get(pathRequest.requestId);
        if (requestBody.data != null) {
            ProxyDataRequest dataRequest = new ProxyDataRequest(requestBody.data);
            Collections.reverse(pathRequest.backtrace);
            dataRequest.path = pathRequest.backtrace;
            try {
                send(dataRequest.path.get(1), dataRequest);
            } catch (ConnectException e) {
                onError(pathRequest.requestId);
            }
        } else {
            onSuccess(requestBody.requestId);
        }
    }

    private void onSuccess(Integer requestId) {
        RequestBody requestBody = requestBodies.get(requestId);
        if (requestBody != null && requestBody.success != null)
            requestBody.success.run();
    }

    private void onProxyDataRequest(ProxyDataRequest dataRequest) {
        int selfIndex = dataRequest.path.indexOf(selfIp);
        if (selfIndex == dataRequest.path.size() - 1) {
            onMessage(dataRequest.data);
        } else {
            try {
                send(dataRequest.path.get(selfIndex + 1), dataRequest);
            } catch (ConnectException ignore) {
            }
        }
    }


    String randomBase64String(int length) {
        Random random = ThreadLocalRandom.current();
        byte[] r = new byte[length];
        random.nextBytes(r);
        return new String(Base64.getEncoder().encode(r));
    }

    public void registration(String domain) {
        sendRequest(domain, null,
                () -> {
                    onRegistrationError(domain);
                },
                () -> {
                    String nextOwnerDomain = randomBase64String(16);
                    String nextOwnerDomainHash = MD5.encode(nextOwnerDomain);
                    Host newHost = new Host(domain, nextOwnerDomainHash, selfIp);
                    RegistrationRequest registrationRequest = new RegistrationRequest(newHost);

                    List<String> similarDomains = findSimilarDomains(domain);
                    if (similarDomains.size() != 0) {
                        while (true) {
                            String target = findFastestDomain(similarDomains);
                            if (target == null)
                                break;
                            try {
                                send(target, registrationRequest);
                                break;
                            } catch (ConnectException e) {
                                similarDomains.remove(target);
                            }
                        }
                    }
                    if (similarDomains.size() == 0) {
                        onRegistrationError(domain);
                    }
                });
    }

    private void onRegistrationError(String domain) {

    }

    void onRegistrationRequest(RegistrationRequest request){
        domains.add(request.host.domain);
        hosts.put(request.host.domain, request.host);
    }



    public void update(String domain, String nextOwnerDomain){
        registration(nextOwnerDomain);
        send();
    }







}

package com.droid.net.http;


import com.droid.djs.fs.Files;
import com.droid.djs.nodes.Node;
import com.droid.djs.nodes.NodeBuilder;
import com.droid.djs.nodes.consts.NodeType;
import com.droid.djs.runner.utils.Parameter;
import com.droid.djs.serialization.node.HttpResponse;
import com.droid.djs.serialization.node.HttpResponseType;
import com.droid.djs.serialization.node.NodeParser;
import com.droid.djs.serialization.node.NodeSerializer;
import com.droid.gdb.map.Crc16;
import com.droid.instance.Instance;
import org.nanohttpd.NanoHTTPD;

import java.io.*;
import java.net.*;
import java.security.InvalidParameterException;
import java.util.*;

public class HttpClientServer extends NanoHTTPD {

    public static class Headers {
        public final static String CONTENT_TYPE = "content-type";
        public static final String AUTHORIZATION = "authorization";
        public static final String AUTHENTICATE = "WWW-Authenticate";
    }

    public static final String FORM_DATA = "application/x-www-form-urlencoded";
    public static int defaultPort = 8080;
    public static String BASIC_AUTH_PREFIX = "Basic ";

    public HttpClientServer(Integer port) throws IOException {
        super(port == null ? defaultPort : port);
        start(0);
    }

    @Override
    public Response serve(IHTTPSession session) {
        super.serve(session);
        long startRequestTime = new Date().getTime();
        Response response = null;
        Instance.connectThreadByPortAdditional(getListeningPort() - defaultPort);
        try {
            String requestContentType = session.getHeaders().get(Headers.CONTENT_TYPE);
            if (requestContentType != null)
                requestContentType = requestContentType.toLowerCase();
            if (session.getMethod() == Method.GET
                    || session.getMethod() == Method.POST && FORM_DATA.equals(requestContentType)) {
                //Authorization: Basic userid:password
                String authorization = session.getHeaders().get(Headers.AUTHORIZATION);
                if (authorization == null || !authorization.startsWith(BASIC_AUTH_PREFIX)) {
                    response = NanoHTTPD.newFixedLengthResponse(Response.Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT, "Need basic auth");
                    response.addHeader(Headers.AUTHENTICATE, "Basic realm=\"Access to the site\"");
                } else {

                    authorization = authorization.substring(BASIC_AUTH_PREFIX.length());
                    authorization = new String(Base64.getDecoder().decode(authorization.getBytes()));
                    String login = authorization.substring(0, authorization.indexOf(":"));
                    String password = authorization.substring(authorization.indexOf(":") + 1);
                    Long access_token = Crc16.getHash(login + password);

                    String urlPath = session.getUri();
                    if (urlPath.equals("") || urlPath.equals("/"))
                        urlPath = "index";

                    Node node = Files.getNodeIfExist(urlPath, access_token);

                    if (node == null) {
                        response = newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "File not Found");
                    } else {
                        Map<String, String> args = parseArguments(session.getQueryParameterString());

                        NodeBuilder builder = new NodeBuilder().set(node);

                        for (Node param : builder.getParams())
                            builder.set(param).setValue(null).commit();

                        for (String argsKey : args.keySet())
                            setParam(node, argsKey, args.get(argsKey));

                        Instance.get().getThreads().run(node, null, false, access_token);

                        if (builder.set(node).isFunction())
                            builder.set(builder.getValueNode());

                        HttpResponse responseData = NodeSerializer.getResponse(builder.getNode());
                        ByteArrayInputStream dataStream = new ByteArrayInputStream(responseData.data);
                        response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, responseData.type, dataStream, responseData.data.length);
                        response.addHeader("content-length", "" + responseData.data.length); // fix nanohttpd issue when content type is define
                    }
                }
            } else if (session.getMethod() == Method.POST) {
                Files.putFile(session.getUri(), session.getInputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response = NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        }

        Instance.disconnectThread();

        if (response == null)
            response = NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "response is empty");

        System.out.println(session.getUri() + " (" + (new Date().getTime() - startRequestTime) + ")");

        return response;
    }

    Map<String, String> parseArguments(String args) {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        if (args != null && !args.equals("")) {
            try {
                for (String pair : args.split("&")) {
                    int idx = pair.indexOf("=");
                    query_pairs.put(pair.substring(0, idx), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
            }
        }
        return query_pairs;
    }

    void setParam(Node node, String key, String value) {
        NodeBuilder builder = new NodeBuilder();
        Node param = null;

        if (key.charAt(0) == '[') {
            // TODO test key = "[key][0][key][0]"
            param = builder.set(node).getParamNode(0);
        } else {
            for (Node paramNode : builder.set(node).getParams())
                if (key.equals(builder.set(paramNode).getTitleString())) {
                    param = paramNode;
                    break;
                }
        }

        if (param != null) {
            Node valueNode;
            if (value.charAt(0) == '!') {
                valueNode = builder.createString(value.substring(1));
            } else if (value.charAt(0) >= '0' && value.charAt(0) <= '9') {
                try {
                    Double numberValue = Double.valueOf(value);
                    valueNode = builder.createNumber(numberValue);
                } catch (NumberFormatException e) {
                    throw new InvalidParameterException();
                }
            } else if ("true".equals(value)) {
                valueNode = builder.createBool(true);
            } else if ("false".equals(value)) {
                valueNode = builder.createBool(false);
            } else
                throw new InvalidParameterException();

            builder.set(param).setValue(valueNode).commit();
        }
    }

    public static String serializeParameters(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(entry.getKey());
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString;
    }

    Map<String, String> buildParameters(Node paramter) {
        NodeBuilder builder = new NodeBuilder();
        Map<String, String> result = new HashMap<>();

        if (builder.set(paramter).isDataVariable()) {
            builder.set(builder.getValueNode());
        }

        if (builder.isString()){
            result.put("[0]", "!" + builder.getData().readString());
        }else if (builder.isNumber()){
            result.put("[0]", builder.getData().readString());
        }else if (builder.isBoolean()){
            result.put("[0]", builder.getData().readString());
        }else /*if (builder.isNULL()){
            result.put("[0]", "!" + builder.getData().readString());
        }else*/ if (builder.set(paramter).isObject()) {
            // TODO test with locals more than 2 levels
            for (Node local : builder.getLocalNodes()) {
                HttpResponse response = NodeSerializer.getResponse(builder.getNode());
                String key = URLEncoder.encode(builder.set(local).getTitleString());
                result.put(key, new String(response.data));
            }
        } else if (builder.set(paramter).isArray()) {
            // TODO add code
        }
        return result;
    }

    public Node request(String host, String path, Node paramter) throws IOException {
        URL url = new URL("http", host, defaultPort, path);
        Map<String, String> parameters = buildParameters(paramter);
        try {
            return request(url, parameters);
        } catch (UnknownHostException ignore) {
            url = new URL("http", Instance.get().proxyHost, (defaultPort + Instance.get().proxyPortAdding), url.getFile());
            try {
                return request(url, parameters);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    // TODO change Map<String, String> parameters to Map<String, InputStream> parameters
    public Node request(URL url, Map<String, String> parameters) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        String authData = Instance.get().login + ":" + Instance.get().password;
        authData = Base64.getEncoder().encodeToString(authData.getBytes());
        con.addRequestProperty(Headers.AUTHORIZATION, BASIC_AUTH_PREFIX + authData);
        con.setDoOutput(true);
        OutputStream outputStream = con.getOutputStream();
        DataOutputStream out = new DataOutputStream(outputStream);
        out.writeBytes(serializeParameters(parameters));
        out.flush();
        out.close();

        InputStream inputStream = con.getInputStream();

        String contentType = con.getHeaderField(Headers.CONTENT_TYPE);
        NodeBuilder builder = new NodeBuilder();
        switch (contentType) {
            case HttpResponseType.TEXT:
                return builder.create(NodeType.STRING).setData(inputStream).commit();
            case HttpResponseType.NUMBER_BASE10:
                return builder.create(NodeType.NUMBER).setData(inputStream).commit();
            case HttpResponseType.BOOLEAN:
                return builder.create(NodeType.BOOLEAN).setData(inputStream).commit();
            case HttpResponseType.NULL:
                return null;
            case HttpResponseType.JSON:
                return NodeParser.fromStream(inputStream);
        }
        // TODO case when http type is not support
        return null;
    }
}

package com.droid.net.http;


import com.droid.djs.builder.NodeBuilder;
import com.droid.djs.consts.NodeType;
import com.droid.djs.fs.Master;
import com.droid.djs.nodes.*;
import com.droid.djs.nodes.DataInputStream;
import com.droid.djs.fs.Files;
import com.droid.djs.treads.Secure;
import com.droid.djs.treads.ThreadPool;
import org.nanohttpd.NanoHTTPD;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpServer extends NanoHTTPD {

    public static int defaultPort = 80;
    public static int debugPort = 8080;
    public static String BASIC_AUTH_PREFIX = "Basic ";

    public HttpServer() {
        this(defaultPort);
    }

    public HttpServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response = null;

        try {
            String requestContentType = session.getHeaders().get(Headers.CONTENT_TYPE);
            if (requestContentType != null)
                requestContentType = requestContentType.toLowerCase();
            if (session.getMethod() == Method.GET
                    || session.getMethod() == Method.POST && ContentType.FORM_DATA.equals(requestContentType)) {
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

                    Node node = Files.getNode(session.getUri(), null);

                    if (node == null) {
                        response = newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "File not Found");
                    } else {
                        Long access_code = Secure.getAccessCode(login, password);

                        node = Files.getNode(session.getUri(), null, access_code);

                        if (node == null) {
                            response = newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "Access denied");
                        } else {
                            Map<String, String> args = null;
                            if (session.getMethod() == Method.POST) {
                                args = parseArguments(session.getInputStream());
                            } else if (session.getMethod() == Method.GET) {
                                args = parseArguments(session.getQueryParameterString());
                            }

                            ArrayList<String> argsKeys = new ArrayList<>(args.keySet());
                            for (String argsKey : argsKeys)
                                setParam(node, argsKey, args.get(argsKey));

                            ThreadPool.getInstance().run(node, null, false, access_code);

                            DataInputStream resultStream = (DataInputStream) getResult(node);
                            if (resultStream != null) {
                                String responseContentType = ContentType.getContentTypeFromName(new NodeBuilder().set(node).getTitleString());
                                response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, responseContentType, resultStream, resultStream.length());
                            }
                        }
                    }
                }
            } else if (session.getMethod() == Method.POST) {
                Files.putFile(session.getUri(), session.getInputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response = NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        } finally {
            if (response != null) {
                response.addHeader("Access-Control-Allow-Origin", "*");
                response.addHeader("Access-Control-Allow-Headers", "Content-Type");
            }
        }

        if (response == null)
            response = NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "response is empty");

        return response;
    }

    private String[] getFileNames(String filename) {
        return new String[]{filename};
    }

    private InputStream getResult(Node resultNode) {
        NodeBuilder builder = new NodeBuilder();
        Node nodeValue = builder.set(resultNode).getValueOrSelf();
        return builder.set(nodeValue).getData();
    }

    Map<String, String> parseArguments(String args) {
        if (args == null)
            return new LinkedHashMap<>();
        return parseArguments(new ByteArrayInputStream(args.getBytes()));
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    Map<String, String> parseArguments(InputStream args) {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = convertStreamToString(args);
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
            }
        }
        return query_pairs;
    }

    ArrayList toList(String str) {
        return new ArrayList();
    }

    void setParam(Node node, String key, String value) {
        NodeBuilder builder = new NodeBuilder();
        Node param = null;
        for (Node paramNode : builder.set(node).getParams())
            if (key.equals(builder.set(paramNode).getTitleString())) {
                param = paramNode;
                break;
            }
        if (param != null) {
            if (param.type == NodeType.NUMBER) {
                Node number = builder.create(NodeType.NUMBER).setData(value).commit();
                builder.set(param).setValue(number).commit();
            } else if (param.type == NodeType.STRING) {
                Node number = builder.create(NodeType.STRING).setData(value).commit();
                builder.set(param).setValue(number).commit();
            } else if (param.type == NodeType.ARRAY) {
                builder.set(param).clearCells();
                for (Object obj : toList(value)) {

                }
            } else if (param.type == NodeType.OBJECT) {

            }
        }
    }
}

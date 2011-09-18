package me.shenfeng.http;

import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public interface HttpClientConstant {

    final static HttpResponse ABORT = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(471, "client abort"));
    final static HttpResponse TOO_LARGE = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(513, "body too large"));
    final static HttpResponse TIMEOUT = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(520, "server timeout"));
    final static HttpResponse CONNECTION_ERROR = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(170, "connecton error"));
    final static HttpResponse CONNECTION_TIMEOUT = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(172, "connecton timeout"));
    final static HttpResponse CONNECTION_RESET = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(175, "connecton reset"));
    final static HttpResponse UNKOWN_HOST = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(171, "unknow host"));
    final static HttpResponse UNKOWN_ERROR = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(180, "unknow error"));
}

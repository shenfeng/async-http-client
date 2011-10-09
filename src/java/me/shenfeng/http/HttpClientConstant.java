package me.shenfeng.http;

import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * home defined http status
 * 
 * @author feng
 * 
 */
public interface HttpClientConstant {

    final static HttpResponse UNKOWN_ERROR = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(150, "unknow error"));

    final static HttpResponse UNKOWN_HOST = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(160, "unknow host"));

    final static HttpResponse CONN_ERROR = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(170, "connecton error"));
    final static HttpResponse CONN_TIMEOUT = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(172, "connecton timeout"));
    final static HttpResponse CONN_RESET = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(175, "connecton reset"));

    final static HttpResponse NULL_LOCATION = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(181, "null location"));
    final static HttpResponse BAD_URL = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(182, "bad url"));
    final static HttpResponse IGNORED_URL = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(183, "ignored url"));

    final static HttpResponse UNKNOWN_CONTENT = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(190, "unknow content type"));

    final static HttpResponse ABORT = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(471, "client abort"));

    final static HttpResponse TOO_LARGE = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(513, "body too large"));
    final static HttpResponse TIMEOUT = new DefaultHttpResponse(HTTP_1_1,
            new HttpResponseStatus(520, "server timeout"));

}

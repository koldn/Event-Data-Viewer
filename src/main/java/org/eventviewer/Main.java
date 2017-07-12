package org.eventviewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Observable;
import io.reactivex.internal.functions.Functions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import org.apache.tika.Tika;
import org.eventviewer.data.DataProvider;

public class Main
{
    public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException
    {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        router.route("/").handler(event ->
        {
            event.response().sendFile("index.html");
        });

        router.route("/read/:fileName").handler(ctx ->
        {
            String fileName = ctx.request().getParam("fileName");
            String address = "fileData." + fileName;
            String filePath = "/tmp/" + fileName;
            Gson gson = new GsonBuilder().serializeNulls().create();
            System.out.println(fileName);

            try
            {
                EventBus eventBus = vertx.eventBus();
                File file = new File(filePath);
                FileInputStream stream = new FileInputStream(file);
                boolean isGzip = new Tika().detect(file).equals("application/gzip");
                DataProvider dataProvider = new DataProvider(isGzip ? new GZIPInputStream(stream) : stream);

                Observable<Object> flowable = Observable.fromIterable(dataProvider);
                flowable.subscribe(o ->
                {
                    eventBus.publish(address, gson.toJson(o));
                }, Functions.ON_ERROR_MISSING, () ->
                {
                    System.out.println("file complete");
                    vertx.fileSystem().deleteBlocking(filePath);
                    eventBus.publish(address, "readingComplete");
                });
            }
            catch (java.io.IOException e)
            {
                ctx.response().end(e.getMessage());
            }
        });

        router.route("/upload").handler(event ->
        {
            HttpServerRequest request = event.request();
            request.setExpectMultipart(true);
            request.uploadHandler(upload ->
            {
                String fileName = upload.filename() + System.currentTimeMillis();
                upload.streamToFileSystem("/tmp/" + fileName);
                event.response().end(fileName);
            });

        });

        BridgeOptions bridge = new BridgeOptions();
        bridge.addOutboundPermitted(new PermittedOptions().setAddressRegex("fileData.*"));

        router.route("/results/*").handler(SockJSHandler.create(vertx).bridge(bridge, event ->
        {
            event.complete(true);
        }));

        router.route().handler(StaticHandler.create());

        vertx.createHttpServer().requestHandler(router::accept).listen(10600);
    }
}
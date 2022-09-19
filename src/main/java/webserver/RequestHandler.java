package webserver;

import java.io.*;
import java.net.Socket;

import http.middlewares.impls.BodyParser;
import http.middlewares.impls.CookieManager;
import http.middlewares.impls.RequestLogger;
import http.request.HttpRequest;
import http.request.SimpleHttpRequest;
import http.response.HttpResponse;
import http.response.SimpleHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.Router;
import routing.SimpleRouter;
import routing.controllerimpls.Authentication;
import routing.controllerimpls.StaticFilesServer;
import routing.controllerimpls.ListAllUsers;
import routing.controllerimpls.UserSignup;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // 요청 패킷 파싱
            HttpRequest requestData = new SimpleHttpRequest(in);

            // IP, PORT 정보 넘겨주기
            requestData.putAdditionalData(HttpRequest.KEY_IP, connection.getInetAddress());
            requestData.putAdditionalData(HttpRequest.KEY_PORT, connection.getPort());

            Router router = new SimpleRouter();

            // 미들웨어 추가
            router.addMiddleware(RequestLogger.getInstance());
            router.addMiddleware(CookieManager.getInstance());
            router.addMiddleware(BodyParser.getInstance());

            // 컨트롤러 추가 (우선순위 순으로 추가하면 됨, 구체적인거 -> 일반적인거 순으로)
            router.addController(new ListAllUsers());
            router.addController(new UserSignup());
            router.addController(new Authentication());
            router.addController(new StaticFilesServer());

            // 응답 생성
            HttpResponse response;
            try {
                response = router.routeAndGetResponse(requestData);
            } catch (Exception e){
                response = SimpleHttpResponse.simpleResponse(500);
                logger.error(e.getMessage());
                e.printStackTrace();
            }

            // 응답 전송
            response.send(out);

        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }

        try {
            connection.close();
        } catch(Exception ee){
            logger.error(ee.getMessage());
        }
    }
}

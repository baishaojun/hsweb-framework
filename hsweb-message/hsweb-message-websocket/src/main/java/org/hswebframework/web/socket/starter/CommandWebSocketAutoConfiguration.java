package org.hswebframework.web.socket.starter;

import org.hswebframework.web.authorization.token.UserTokenManager;
import org.hswebframework.web.concurrent.counter.CounterManager;
import org.hswebframework.web.message.Messager;
import org.hswebframework.web.socket.WebSocketSessionListener;
import org.hswebframework.web.socket.authorize.AuthorizeCommandProcessor;
import org.hswebframework.web.socket.authorize.SessionIdWebSocketTokenParser;
import org.hswebframework.web.socket.authorize.WebSocketTokenParser;
import org.hswebframework.web.socket.authorize.XAccessTokenParser;
import org.hswebframework.web.socket.handler.CommandWebSocketMessageDispatcher;
import org.hswebframework.web.socket.message.DefaultWebSocketMessager;
import org.hswebframework.web.socket.message.WebSocketMessager;
import org.hswebframework.web.socket.processor.DefaultCommandProcessorContainer;
import org.hswebframework.web.socket.processor.CommandProcessor;
import org.hswebframework.web.socket.processor.CommandProcessorContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurationSupport;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * @author zhouhao
 */
@Configuration
public class CommandWebSocketAutoConfiguration {

    @Bean
    public SessionIdWebSocketTokenParser sessionIdWebSocketTokenParser(){
        return new SessionIdWebSocketTokenParser();
    }

    @Bean
    public XAccessTokenParser xAccessTokenParser(){
        return new XAccessTokenParser();
    }

    @Bean
    @ConditionalOnBean(UserTokenManager.class)
    public AuthorizeCommandProcessor authorizeCommandProcessor(UserTokenManager userTokenManager){
        return new AuthorizeCommandProcessor(userTokenManager);
    }

    @Configuration
    @ConditionalOnMissingBean(CommandProcessorContainer.class)
    public static class WebSocketProcessorContainerConfiguration {
        @Autowired(required = false)
        private List<CommandProcessor> commandProcessors;

        @Bean(destroyMethod = "destroy")
        public DefaultCommandProcessorContainer defaultWebSocketProcessorContainer() {
            DefaultCommandProcessorContainer container = new DefaultCommandProcessorContainer();
            if (commandProcessors != null) {
                commandProcessors.forEach(container::install);
            }
            return container;
        }
    }

    @Configuration
    @ConditionalOnBean(Messager.class)
    @ConditionalOnMissingBean(WebSocketMessager.class)
    public static class WebSocketMessagerConfiguration {
        @Autowired(required = false)
        private CounterManager counterManager;

        @Bean
        public WebSocketMessager webSocketMessager(Messager messager) {
            return new DefaultWebSocketMessager(messager,counterManager);
        }
    }

    @Configuration
    public static class HandlerConfigruation extends WebSocketConfigurationSupport {
        @Autowired(required = false)
        private UserTokenManager userTokenManager;

        @Autowired(required = false)
        private List<WebSocketSessionListener> webSocketSessionListeners;

        @Autowired(required = false)
        private List<WebSocketTokenParser> webSocketTokenParsers;

        @Autowired
        private CommandProcessorContainer commandProcessorContainer;

        @Override
        protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            CommandWebSocketMessageDispatcher dispatcher = new CommandWebSocketMessageDispatcher();
            dispatcher.setProcessorContainer(commandProcessorContainer);
            dispatcher.setUserTokenManager(userTokenManager);
            dispatcher.setWebSocketSessionListeners(webSocketSessionListeners);
            dispatcher.setTokenParsers(webSocketTokenParsers);
            registry.addHandler(dispatcher, "/sockjs")
                    .withSockJS()
                    .setSessionCookieNeeded(true);
            registry.addHandler(dispatcher, "/socket");
        }
    }

}

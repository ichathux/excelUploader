//package com.controlunion.excelUploader.fileUpload.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.Data;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.MessageHandler;
//import org.springframework.messaging.SubscribableChannel;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.messaging.simp.config.MessageBrokerRegistry;
//import org.springframework.messaging.support.GenericMessage;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketHandler;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.config.annotation.*;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
//@Configuration
//@EnableWebSocketMessageBroker
//public class WebSocketConfig implements WebSocketMessageBrokerConfigurer  {
////    @Override
////    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//////        registry.addHandler(tradeWebSocketHandler(), "/stocks").setAllowedOrigins("*");
//////        registry.addHandler(tradeWebSocketHandler(), "/ws").setAllowedOrigins("*");
////        registry.addHandler(tradeWebSocketHandler(), "/uploads").setAllowedOrigins("*");
////    }
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        config.enableSimpleBroker("/topic", "/queue");
//        config.setApplicationDestinationPrefixes("/app");
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/uploads").setAllowedOrigins("http://localhost:4200").withSockJS();
//    }
//    @Bean
//    public WebSocketHandler tradeWebSocketHandler() {
//        return new TradeWebSocketHandler(messagingTemplate());
//    }
//    @Bean
//    public SimpMessagingTemplate messagingTemplate() {
//        // Create a dummy SubscribableChannel as a message channel
//        SubscribableChannel dummyChannel = new SubscribableChannel() {
//            @Override
//            public boolean send(Message<?> message, long timeout) {
//                // Handle the message as needed (in this case, just print it)
//                System.out.println("Received message on dummy channel: " + message);
//                return true;
//            }
//
//            @Override
//            public boolean subscribe(MessageHandler handler) {
//                return false;
//            }
//
//            @Override
//            public boolean unsubscribe(MessageHandler handler) {
//                return false;
//            }
//
//
//        };
//
//        // Send a dummy message to the channel
//        dummyChannel.send(new GenericMessage<>("Hello, Dummy Message!"));
//
//        // Create SimpMessagingTemplate with the dummy message channel
//        return new SimpMessagingTemplate(dummyChannel);
//    }
//
//}
//
//
//@RequiredArgsConstructor
//class TradeWebSocketHandler extends TextWebSocketHandler {
//    private final SimpMessagingTemplate messagingTemplate;
//
//    @Override
//    public void handleTextMessage(WebSocketSession session,TextMessage message) throws Exception {
//        String payload = message.getPayload();
//
//        for (int i = 0 ; i < 10 ; i++){
//            messagingTemplate.convertAndSendToUser(session.getId(), "/queue/specific-user", "Hello, Specific User!");
//
//            // Or send a message to a topic
//            messagingTemplate.convertAndSend("/topic/my-topic", "Hello, Topic Subscribers!");
//
//            Thread.sleep(1000);
//        }
//        // Send a message to a specific user or topic using SimpMessagingTemplate
//        messagingTemplate.convertAndSendToUser(session.getId(), "/queue/specific-user", "Hello, Specific User!");
//
//        // Or send a message to a topic
//        messagingTemplate.convertAndSend("/topic/my-topic", "Hello, Topic Subscribers!");
//
//    }
//}
//
//// Stock Modal Class
//@Data
//class Stock {
//    String name;
//    String icon;
//    float price;
//    boolean increased;
//
//    public Stock(String name, String icon, float price) {
//        this.name = name;
//        this.icon = icon;
//        this.price = price;
//    }
//
//    // Getters & setters ...
//}
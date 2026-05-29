package com.pfe.predictive.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration for Real-Time Machine Telemetry Streaming
 * 
 * Configures STOMP over WebSocket for broadcasting machine telemetry data.
 * 
 * Endpoints:
 * - /ws-machine: WebSocket handshake endpoint (with SockJS fallback)
 * - /ws-nlp: Legacy compatibility handshake endpoint for the NLP frontend
 * - /topic/machines: Broadcast destination for machine telemetry
 * 
 * Protocol: STOMP (Simple Text Oriented Messaging Protocol)
 * Fallback: SockJS (for browsers that don't support WebSocket)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for broadcasting.
     * 
     * - /topic: Broker prefix for broadcasting to all subscribers
     * - /app: Application destination prefix for client-to-server messages
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for broadcasting
        config.enableSimpleBroker("/topic");
        
        // Application destination prefix (for client-to-server messages)
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register STOMP endpoints for WebSocket connections.
     * 
     * Endpoint: /ws-machine
     * - Clients connect to: ws://localhost:8080/ws-machine
     * - SockJS fallback: http://localhost:8080/ws-machine/info
     * 
     * CORS: Allowed for all origins (configure for production)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-machine")
                .setAllowedOriginPatterns("*")  // Allow all origins (configure for production)
                .withSockJS();                   // Enable SockJS fallback

        registry.addEndpoint("/ws-nlp")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}

import { useCallback, useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
// Use SockJS for WebSocket fallback
import SockJS from "sockjs-client";
import type { IMessage } from "@stomp/stompjs";
import { useAuth } from "../context/AuthContext";

type EventHandler = (event: any) => void;

function resolveBrokerUrl() {
  // Prefer Vite env if provided, fallback to backend default (localhost:8080/sps)
  // Example: VITE_WS_URL=ws://localhost:8080/sps/ws
  const envUrl = (import.meta as any).env?.VITE_WS_URL as string | undefined;
  if (envUrl) return envUrl;

  // If frontend is served by backend in production, window.location.host will work.
  // In dev, backend is typically 8080 with /sps context path.
  const host = window.location.hostname;
  return `ws://${host}:8080/sps/ws`;
}

class WebSocketService {
  private client: Client | null = null;
  private subscribers: Map<string, Set<EventHandler>> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000; // 3 seconds

  public connect() {
    // Read JWT token from localStorage
    const token = localStorage.getItem('token');

    // If no token, do not connect (user not logged in)
    if (!token) return;

    // Avoid creating multiple clients
    if (this.client?.active) return;

    this.client = new Client({
      // Use SockJS to ensure STOMP frames work reliably with Spring's STOMP endpoint
      webSocketFactory: () =>
        new SockJS(resolveBrokerUrl().replace(/^ws/, "http")) as any,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      // Disable STOMP internal debug spam; rely on our own logs
      debug: () => { },
      reconnectDelay: 0, // we handle reconnect ourselves
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log("[WebSocket] STOMP Connected to", resolveBrokerUrl());
        this.reconnectAttempts = 0;
        this.resubscribeAll();
      },
      onStompError: (frame) => {
        console.error(
          "[WebSocket] STOMP Broker reported error:",
          frame.headers["message"]
        );
        console.error("Additional details:", frame.body);
      },
      onWebSocketError: (evt) => {
        console.error("[WebSocket] WebSocket error event:", evt);
      },
      onWebSocketClose: (evt) => {
        console.log("[WebSocket] WebSocket closed", evt);
        this.attemptReconnect();
      },
    });

    this.client.activate();
  }

  private attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(
        `[WebSocket] Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`
      );

      setTimeout(() => {
        this.connect();
      }, this.reconnectDelay);
    } else {
      console.error("[WebSocket] Max reconnection attempts reached");
    }
  }

  private resubscribeAll() {
    this.subscribers.forEach((handlers, destination) => {
      handlers.forEach((handler) => {
        this.subscribe(destination, handler);
      });
    });
  }

  subscribe(destination: string, handler: EventHandler): () => void {
    if (!this.client || !this.client.connected) {
      // Store subscription to be activated when connected
      if (!this.subscribers.has(destination)) {
        this.subscribers.set(destination, new Set());
      }
      this.subscribers.get(destination)?.add(handler);

      // Return cleanup function that removes the handler
      return () => {
        this.subscribers.get(destination)?.delete(handler);
      };
    }

    // If already connected, subscribe immediately
    const subscription = this.client.subscribe(
      destination,
      (message: IMessage) => {
        try {
          const data = JSON.parse(message.body);
          handler(data);
        } catch (error) {
          console.error("[WebSocket] Error parsing message:", error);
        }
      }
    );

    // Store the subscription for reconnection
    if (!this.subscribers.has(destination)) {
      this.subscribers.set(destination, new Set());
    }
    this.subscribers.get(destination)?.add(handler);

    // Return cleanup function
    return () => {
      subscription.unsubscribe();
      this.subscribers.get(destination)?.delete(handler);
    };
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }
}

// Create a single instance
const webSocketService = new WebSocketService();

// React hook for using WebSocket
export function useWebSocket() {
  const { isAuthenticated } = useAuth();
  const subscriptions = useRef<Array<() => void>>([]);

  // Clean up on unmount
  useEffect(() => {
    return () => {
      subscriptions.current.forEach((unsubscribe) => unsubscribe());
      subscriptions.current = [];
    };
  }, []);

  // Reconnect when authentication state changes
  useEffect(() => {
    if (isAuthenticated) {
      webSocketService.connect();
    } else {
      webSocketService.disconnect();
    }
  }, [isAuthenticated]);

  const subscribe = useCallback(
    (destination: string, handler: EventHandler) => {
      if (!isAuthenticated) return () => { };

      const unsubscribe = webSocketService.subscribe(destination, handler);
      subscriptions.current.push(unsubscribe);

      return () => {
        unsubscribe();
        subscriptions.current = subscriptions.current.filter(
          (sub) => sub !== unsubscribe
        );
      };
    },
    [isAuthenticated]
  );

  return { subscribe };
}

export default webSocketService;

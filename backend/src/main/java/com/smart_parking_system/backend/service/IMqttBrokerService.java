package com.smart_parking_system.backend.service;

public interface IMqttBrokerService {

    void createBrokerUser(String username, String password);

    void deleteBrokerUser(String username);

    void setUserAcl(String username, Integer userId);
}

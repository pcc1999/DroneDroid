package com.upc.dronedroid.models;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;

public class User {
    public String username;
    public boolean needsPassword;
    public String password;
    public boolean canCreateRoutes;
    public boolean canLoadRoutes;
    public boolean canDeleteRoutes;

    public User(String username, boolean needsPassword, String password, boolean canCreateRoutes, boolean canLoadRoutes, boolean canDeleteRoutes) {
        this.username = username;
        this.needsPassword = needsPassword;
        this.password = password;
        this.canCreateRoutes = canCreateRoutes;
        this.canLoadRoutes = canLoadRoutes;
        this.canDeleteRoutes = canDeleteRoutes;
    }

    public String getUsername() {
        return username;
    }

    public static List<User> getUsersFromJSONArray(JSONArray jsonArray) throws JSONException {
        List<User> usersList = new LinkedList<>();
        for(int i = 0; i < jsonArray.length(); i++){
            User newUser = new User(
                    jsonArray.getJSONObject(i).getString("username"),
                    jsonArray.getJSONObject(i).getBoolean("needsPassword"),
                    jsonArray.getJSONObject(i).getString("password"),
                    jsonArray.getJSONObject(i).getBoolean("canCreateRoutes"),
                    jsonArray.getJSONObject(i).getBoolean("canLoadRoutes"),
                    jsonArray.getJSONObject(i).getBoolean("canDeleteRoutes")
            );
            usersList.add(newUser);
        }
        return usersList;
    }
}

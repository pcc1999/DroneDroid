package com.upc.dronedroid.utils;

import android.app.Activity;
import android.content.pm.PackageManager;

import com.upc.dronedroid.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class PermissionUtil {

    public static boolean verifyLocalPermissions(int[] grantResults) {
        // At least one result must be checked.
        if (grantResults.length < 1) {
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static List<User> getAvailableUsers(Activity activity) {
        try {
            String json = AssetsUtil.loadJSONFromAsset(activity, "usersPermissions.json");
            assert json != null;
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("users");
            return User.getUsersFromJSONArray(jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean checkAuth(String uid, String roleName) {
        //This function checks if a user can perform an action by calling retrieveRoles function and
        //looking for it into the roles of the user
        List<String> rolesList;
        rolesList = retrieveRoles(uid);
        return retrieveRoles(uid).contains(roleName);
    }

    public static List<String> retrieveRoles(String uid) {
        //This function performs an API Call to retrieve the roles assignation for the given user
        List<String> rolesList = new LinkedList<>();
        return rolesList;
    }

    public static boolean checkPassword(User selectedUser, String password) {
        return selectedUser.password.equals(password);
    }
}

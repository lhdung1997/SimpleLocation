package com.example.hoangdung.simplelocation;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WelcomeActivity extends AppCompatActivity {
    //For Debugging
    private final String TAG = WelcomeActivity.class.getSimpleName();
    //Request code
    private final int FINE_LOCATION_REQUEST = 0;
    //Delay time
    private final int delayTime = 2000;
    //Login Button
    public @BindView(R.id.loginBtn) Button mLoginBtn;
    private CallbackManager mCallbackManager;
    //Facebook and Firebase
    private FirebaseAuth mAuth;
    //Read Permission
    private List<String> readPermissions = Arrays.asList(
            "email","public_profile");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        ButterKnife.bind(this);
        initializeFirebaseAuth();
        getLocationPermission();
        loginSetup();

    }

    /**
     * Request users to permit ACCESS_FINE_LOCATION
     */
    private void getLocationPermission(){
        Log.d(TAG,"getLocationPermission");
        //If the applications is not permitted, ask for permission
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},FINE_LOCATION_REQUEST);
        }
    }
    /**
     *
     */
    private void loginSetup(){
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLogin();
            }
        });
        //if already loggined
        if(checkAlreadyLoggined()){
            mLoginBtn.setVisibility(View.INVISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToMapsActivity();
                    finish();
                }
            }, delayTime);
            return;
        }
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest graphRequest =  GraphRequest.newMeRequest(
                        loginResult.getAccessToken()
                        , new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Intent intent = new Intent(WelcomeActivity.this,MapsActivity.class);
                                try {
                                    //Store User Information From Facebook into SharePreferences
                                    SharedPreferences sharedPreferences = getSharedPreferences("UserInfo",MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("email",object.getString("email"));
                                    editor.putString("first_name",object.getString("first_name"));
                                    editor.putString("last_name",object.getString("last_name"));
                                    editor.putString("picture",object.getJSONObject("picture").getJSONObject("data").getString("url"));
                                    editor.commit();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                goToMapsActivity();
                                finish();
                            }
                        }
                );//graphRequest
                Bundle params = new Bundle();
                params.putString("fields","email,first_name,last_name,picture");
                graphRequest.setParameters(params);
                graphRequest.executeAsync();
                //FireBase Authentication
                handleFacebookAccessToken(loginResult.getAccessToken());

            }
            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
    }

    private boolean checkAlreadyLoggined(){
        //If the user is already loggin, go straight to the MapsActivity
        return AccessToken.getCurrentAccessToken()!= null && Profile.getCurrentProfile() != null;
    }
    private void goToMapsActivity(){
        Intent intent = new Intent(WelcomeActivity.this,MapsActivity.class);
        WelcomeActivity.this.startActivity(intent);
    }
    private void startLogin(){
        LoginManager.getInstance().logInWithReadPermissions(WelcomeActivity.this,readPermissions);
    }
    //------------------------------Facebook and Firebase--------------------------------------
    private void initializeFirebaseAuth(){
        mAuth = FirebaseAuth.getInstance();
    }
    private void handleFacebookAccessToken(AccessToken token){
        Log.d(TAG,"handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG,"signInWithCredential:success");
                        }else{
                            Log.d(TAG,"signInWithCredential:failure");
                        }
                    }
                });
    }
    /**
     * Request Result Callback Method
     * @param requestCode
     * @param permissions
     * @param grantResults if fail, this param is empty
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case FINE_LOCATION_REQUEST:{
                if(!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                    finish();
                else{

                }
            }//Case Fine_Location_Request
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode,resultCode,data);
    }
}

/*
 * Copyright 2004 - sigseg, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sigseg.android.funfriends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.R;
import com.facebook.android.Util;
import com.sigseg.android.funfriends.SessionEvents.AuthListener;
import com.sigseg.android.funfriends.SessionEvents.LogoutListener;

public class FunFriends extends Activity implements OnTouchListener {


    public static final String APP_ID = "271326752972714";
    private static final String FUNCOUNT = "funcount";

    private LoginButton mLoginButton;
    private ImageView mUserPic;
    private WebView mLeftWebView;
    private WebView mRightWebView;
    private ProgressBar mProgressBar1;
    private TextView mNOFM;
    private Handler mHandler;
    
    
    ProgressDialog dialog;
    protected static JSONArray jsonArray = null;
    
    private int nofmN;
    private static final int NOFMM = 10;

    final static int AUTHORIZE_ACTIVITY_RESULT_CODE = 0;

    private ProgressDialog progressDialog;

    String[] permissions = { "offline_access", "publish_stream", "user_photos", "publish_checkins", "photo_upload" };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (APP_ID == null) {
            Util.showAlert(this, "Warning", "Facebook Applicaton ID must be "
                    + "specified before running this example: see FbAPIs.java");
            return;
        }

        setContentView(R.layout.main);
        mHandler = new Handler();

        mUserPic = (ImageView) FunFriends.this.findViewById(R.id.user_pic);
        mLeftWebView = (WebView) findViewById(R.id.leftWebView);
        mRightWebView = (WebView) findViewById(R.id.rightWebView);
        mProgressBar1 = (ProgressBar) findViewById(R.id.progressBar1);
        mNOFM = (TextView) findViewById(R.id.nOfM);

        // Create the Facebook Object using the app id.
        Utility.mFacebook = new Facebook(APP_ID);
        // Instantiate the asynrunner object for asynchronous api calls.
        Utility.mAsyncRunner = new AsyncFacebookRunner(Utility.mFacebook);

        mLoginButton = (LoginButton) findViewById(R.id.login);

        // restore session if one exists
        SessionStore.restore(Utility.mFacebook, this);
        SessionEvents.addAuthListener(new FbAPIsAuthListener());
        SessionEvents.addLogoutListener(new FbAPIsLogoutListener());

        /*
         * Source Tag: login_tag
         */
        mLoginButton.init(this, AUTHORIZE_ACTIVITY_RESULT_CODE, Utility.mFacebook, permissions);

        if (Utility.mFacebook.isSessionValid()) {
            requestUserData();
        }

        progressDialog = new ProgressDialog(this);
        
        mLeftWebView.setOnTouchListener(this);
        mRightWebView.setOnTouchListener(this);
        
    }
    
    public boolean onTouch(View v, MotionEvent event){
    	if (event.getAction() == MotionEvent.ACTION_UP){
	    	if (v.getTag() instanceof Integer){
	    		int index = (Integer) v.getTag();
	    		if (jsonArray!=null){
	    			int funcount;
					try {
						funcount = jsonArray.getJSONObject(index).getInt(FUNCOUNT);
		    			jsonArray.getJSONObject(index).put(FUNCOUNT, funcount+1);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    			nofmN++;
	    			setUI();
	    		}
	    	}
    	}
    	return true;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if(Utility.mFacebook != null) {
            if (!Utility.mFacebook.isSessionValid()) {
                mUserPic.setImageBitmap(null);
            } else {
                Utility.mFacebook.extendAccessTokenIfNeeded(this, null);

                progressDialog.setTitle("Getting Friends");
                progressDialog.setMessage("Please wait while I get a list of your Facebook Friends");
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.show();

                Bundle params = new Bundle();
                params.putString("fields", "name, picture, location");
                Utility.mAsyncRunner.request("me/friends", params,
                        new FriendsRequestSuperListener());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        /*
         * if this is the activity result from authorization flow, do a call
         * back to authorizeCallback Source Tag: login_tag
         */
            case AUTHORIZE_ACTIVITY_RESULT_CODE: {
                Utility.mFacebook.authorizeCallback(requestCode, resultCode, data);
                break;
            }
        }
    }

    /*
     * callback after friends are fetched via me/friends or fql query.
     */
    public class FriendsRequestSuperListener extends BaseRequestListener {

        @Override
        public void onComplete(final String response, final Object state) {
            progressDialog.dismiss();
            try {
				jsonArray = new JSONObject(response).getJSONArray("data");
				for(int i=0; i<jsonArray.length();i++){
					jsonArray.getJSONObject(i).put(FUNCOUNT, 0);
				}
				nofmN = 1;
	            mHandler.post(new Runnable() {
	                @Override
	                public void run() {
	    				setUI();
	                }
	            });

			} catch (JSONException e) {
	            return;
			}
        }

        public void onFacebookError(FacebookError error) {
            dialog.dismiss();
            Toast.makeText(getApplicationContext(), "Facebook Error: " + error.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setUI(){
    	if (nofmN<=NOFMM){
	    	WebView[] views = {mLeftWebView, mRightWebView};
	        Random generator = new Random();
	    	for (WebView wv : views){
	    		int i = generator.nextInt(jsonArray.length());
				String url;
				try {
					url = jsonArray.getJSONObject(i).getJSONObject("picture").getJSONObject("data").getString("url");
					String html = String.format("<img src='%s' width='100%%' height='100%%' />",url);
					wv.loadData(html,"text/html","utf-8");
					wv.setTag((Integer)i);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
			mNOFM.setText(String.format("%d / %d", nofmN, NOFMM));
			mProgressBar1.setMax(NOFMM);
			mProgressBar1.setProgress(nofmN);
    	} else {
    		List<String> ff = new ArrayList<String>();
    		for(int i=0; i<jsonArray.length();i++){
    			int funcount=0;
    			try {
					funcount = jsonArray.getJSONObject(i).getInt(FUNCOUNT);
				} catch (JSONException e) {}
    			if (funcount>0){
    				ff.add(String.format("%02d:%08d",funcount,i));
    			}
    		}
			Collections.sort(ff);
			for(String s : ff){
				Log.i("jl",s);
			}
			StringBuilder l = new StringBuilder();
			StringBuilder r = new StringBuilder();
			StringBuilder sb;
			String s;
			s = "<table>";
			r.append(s);
			l.append(s);
			int rows=0;
			for(int i=ff.size()-1;i>-1;i--){
				s = ff.get(i);
				int c = Integer.parseInt(s.substring(0, 2));
				int index = Integer.parseInt(s.substring(3));
				String name;
				try {
					name = jsonArray.getJSONObject(index).getString("name");
					sb = l;
					if (rows++>=5)
						sb = r;
					sb.append(String.format("<tr><td>%d</td><td>%s</td><tr>",c,name));
				} catch (JSONException e) {}
			}
			s = "</table>";
			r.append(s);
			l.append(s);
			mLeftWebView.loadData(l.toString(),"text/html",null);
			mRightWebView.loadData(r.toString(),"text/html",null);
    	}
    }
    
    public class FQLRequestListener extends BaseRequestListener {

        @Override
        public void onComplete(final String response, final Object state) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Response: " + response,
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        public void onFacebookError(FacebookError error) {
            Toast.makeText(getApplicationContext(), "Facebook Error: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Callback for fetching current user's name, picture, uid.
     */
    public class UserRequestListener extends BaseRequestListener {

        @Override
        public void onComplete(final String response, final Object state) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(response);

                final String picURL = jsonObject.getString("picture");
                Utility.userUID = jsonObject.getString("id");

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mUserPic.setImageBitmap(Utility.getBitmap(picURL));
                    }
                });

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    /*
     * The Callback for notifying the application when authorization succeeds or
     * fails.
     */

    public class FbAPIsAuthListener implements AuthListener {

        @Override
        public void onAuthSucceed() {
            requestUserData();
        }

        @Override
        public void onAuthFail(String error) {
        }
    }

    /*
     * The Callback for notifying the application when log out starts and
     * finishes.
     */
    public class FbAPIsLogoutListener implements LogoutListener {
        @Override
        public void onLogoutBegin() {
        }

        @Override
        public void onLogoutFinish() {
            mUserPic.setImageBitmap(null);
        }
    }

    /*
     * Request user name, and picture to show on the main screen.
     */
    public void requestUserData() {
        Bundle params = new Bundle();
        params.putString("fields", "name, picture");
        Utility.mAsyncRunner.request("me", params, new UserRequestListener());
    }

}

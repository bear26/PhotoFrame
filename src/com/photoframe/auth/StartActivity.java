package com.photoframe.auth;

/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

import android.accounts.*;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.Toast;
import com.photoframe.activity.MainActivity;
import com.photoframe.util.MyConst;
import com.photoframe.R;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartActivity extends FragmentActivity {


    private static final int GET_ACCOUNT_CREDS_INTENT = 778867;

    public static final String CLIENT_ID = "5e8bb34f8d524b93a433867a4b0167b8";
    public static final String CLIENT_SECRET = "7bab04fdf3514bd182dee72a217b3573";

    public static final String ACCOUNT_TYPE = "com.yandex";
    public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id=" + CLIENT_ID;
    private static final String ACTION_ADD_ACCOUNT = "com.yandex.intent.ADD_ACCOUNT";
    private static final String KEY_CLIENT_SECRET = "clientSecret";

    public static String USERNAME = "username";
    public static String TOKEN = "token";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getData() != null) {
            onLogin();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = preferences.getString(TOKEN, null);
        if (token == null) {
            getToken();
            return;
        }

        if (savedInstanceState == null) {
            startFragment();
        }
    }

    private void startFragment() {
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        intent.putExtra(MyConst.DIRECTORY_PATH, "/");
        startActivity(intent);
    }

    private void onLogin() {
        Uri data = getIntent().getData();
        setIntent(null);
        Pattern pattern = Pattern.compile("access_token=(.*?)(&|$)");
        Matcher matcher = pattern.matcher(data.toString());
        if (matcher.find()) {
            final String token = matcher.group(1);
            if (!TextUtils.isEmpty(token)) {
                saveToken(token);
            }
        }
    }

    private void saveToken(String token) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(USERNAME, "");
        editor.putString(TOKEN, token);
        editor.commit();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_ACCOUNT_CREDS_INTENT) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                String name = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
                String type = bundle.getString(AccountManager.KEY_ACCOUNT_TYPE);
                Account account = new Account(name, type);
                getAuthToken(account);
            }
        }
    }

    private void getAuthToken(Account account) {
        AccountManager systemAccountManager = AccountManager.get(getApplicationContext());
        Bundle options = new Bundle();
        options.putString(KEY_CLIENT_SECRET, CLIENT_SECRET);
        systemAccountManager.getAuthToken(account, CLIENT_ID, options, this, new GetAuthTokenCallback(), null);
    }

    private void getToken() {
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        if (accounts != null && accounts.length > 0) {
            Account account = accounts[0];
            getAuthToken(account);
            return;
        }

        for (AuthenticatorDescription authDesc : accountManager.getAuthenticatorTypes()) {
            if (ACCOUNT_TYPE.equals(authDesc.type)) {
                Intent intent = new Intent(ACTION_ADD_ACCOUNT);
                startActivityForResult(intent, GET_ACCOUNT_CREDS_INTENT);
                return;
            }
        }

        new AuthDialogFragment().show(getSupportFragmentManager(), "auth");
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();

                String message = (String) bundle.get(AccountManager.KEY_ERROR_MESSAGE);
                if (message != null) {
                    Toast.makeText(StartActivity.this, message, Toast.LENGTH_LONG).show();
                }

                Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (intent != null) {
                    startActivityForResult(intent, GET_ACCOUNT_CREDS_INTENT);
                } else {
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    saveToken(token);
                    startFragment();
                }
            } catch (OperationCanceledException ex) {
                Toast.makeText(StartActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
            } catch (AuthenticatorException ex) {
                Toast.makeText(StartActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
            } catch (IOException ex) {
                Toast.makeText(StartActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class AuthDialogFragment extends DialogFragment {

        public AuthDialogFragment() {
            super();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.auth_dialog_title)
                    .setMessage(R.string.auth_dialog_message)
                    .setPositiveButton(R.string.auth_dialog_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL)));
                        }
                    })
                    .setNegativeButton(R.string.auth_dialog_negative_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            getActivity().finish();
                        }
                    })
                    .create();
        }
    }


}

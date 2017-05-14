package com.example.nickvanniekerk.grandfathered;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.compat.*;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class LoginActivity extends AccountHelper {

    private Button btnLogin;
    private EditText txtEmail;
    private EditText txtPassword;
    private TextView txtForgotPassword;
    private TextView txtSignUp;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeComponents();
    }

    /**
     * Called on load to assign interface variables to their respective views
     */
    private void initializeComponents() {
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        txtPassword = (EditText) findViewById(R.id.txtPassword);
        txtForgotPassword = (TextView) findViewById(R.id.txtForgotPassword);
        txtSignUp = (TextView) findViewById(R.id.txtSignUp);

        addListeners();
    }

    /**
     * Set action listeners on appropriate UI elements
     */
    private void addListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnLoginClicked();
            }
        });

        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forgotPasswordClicked();
            }
        });

        txtSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUpClicked();
            }
        });
    }

    /**
     * Navigate to the sign up activity
     */
    private void signUpClicked() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        startActivity(new Intent(this, SignupActivity.class));
    }

    /**
     * Navigate to the forgot password activity
     */
    private void forgotPasswordClicked() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        startActivity(new Intent(this, PasswordResetActivity.class));
    }

    /**
     * Attempt to log in a user
     */
    private void btnLoginClicked() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        if (validateLoginRequirements()) {
            signInWithEmailAndPassword(txtEmail.getText().toString().trim(), txtPassword.getText().toString().trim());
        }
    }

    public void signInWithEmailAndPassword(final String email, final String password) {
        super.mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            tryLoginMigratedUser(email, password);
                        } else {
                            openMain();
                        }
                    }
                });
    }

    @NonNull
    private static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void tryLoginMigratedUser(final String email, final String password) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        Query emailExists = ref.child("migrated_users").orderByChild("email").equalTo(email);

        emailExists.addValueEventListener( new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    migratedLogin(email, password);
                } else {
                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void migratedLogin(final String email, final String password) {
        String secretKey = BuildConfig.MigrationSecretKey;
        super.mAuth.signInWithEmailAndPassword(email, md5(email + secretKey))
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        } else {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                user.updatePassword(password).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                                        ref.child("migrated_users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(
                                                new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        dataSnapshot.getRef().setValue(null);
                                                        openMain();
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                        Log.w(TAG, "signInWithEmail:failed", databaseError.toException());
                                                    }
                                                });
                                    }
                                });
                            }
                        }
                    }
                });
    }

    private void openMain() {
        Toast.makeText(LoginActivity.this, "Successfully logged in.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
    }
    /**
     * @return validity of provided information
     */
    private Boolean validateLoginRequirements() {
        Boolean valid = false;
        if (txtEmail.getText() != null || !txtEmail.getText().toString().trim().equals("") ||
                txtPassword.getText() != null || !txtPassword.getText().toString().trim().equals("")) {
            if (isValidEmail(txtEmail.getText().toString().trim())) {
                valid = true;
            } else {
                Toast.makeText(this, "Invalid email address, please try again.", Toast.LENGTH_LONG).show();
                clearEditText(txtEmail);
            }
        } else {
            Toast.makeText(this, "Please make sure you fill in all the fields.", Toast.LENGTH_LONG).show();
        }
        return valid;
    }

    /**
     * @param editText EdiText for clearing
     */
    private void clearEditText(EditText editText) {
        editText.setText("");
    }

    /**
     * @param target String to be validated
     * @return validity of email address
     */
    public static boolean isValidEmail(CharSequence target) {
        return Pattern.compile("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$").matcher(target).matches();
    }
}

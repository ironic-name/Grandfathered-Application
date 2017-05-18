package com.example.nickvanniekerk.grandfathered;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.mindrot.jbcrypt.BCrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class LoginActivity extends AccountHelper {

    private Button btnLogin;
    private EditText txtEmail;
    private EditText txtPassword;
    private TextView txtForgotPassword;
    private TextView txtSignUp;
    private ProgressBar loadingProgressBar;
    private static final String TAG = "LoginActivity";
    DatabaseReference ref;

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

        loadingProgressBar = (ProgressBar) findViewById(R.id.loadingProgress);
        loadingProgressBar.setVisibility(View.GONE);

        ref = FirebaseDatabase.getInstance().getReference();

        addListeners();
    }

    /**
     * Set action listeners on appropriate UI elements
     */
    private void addListeners() {
        btnLogin.setOnClickListener(view -> btnLoginClicked());
        txtForgotPassword.setOnClickListener(view -> forgotPasswordClicked());
        txtSignUp.setOnClickListener(view -> signUpClicked());
    }

    /**
     * Navigate to the sign up activity
     */
    private void signUpClicked() {
        startActivity(new Intent(this, SignupActivity.class));
    }

    /**
     * Navigate to the forgot password activity
     */
    private void forgotPasswordClicked() {
        startActivity(new Intent(this, PasswordResetActivity.class));
    }

    /**
     * Attempt to log in a user
     */
    private void btnLoginClicked() {
        if (validateLoginRequirements()) {
            loadingProgressBar.setIndeterminate(true);
            startLoader();
            signInWithEmailAndPassword(
                    txtEmail.getText().toString().trim(),
                    txtPassword.getText().toString().trim()
            );
        }
    }

    /**
     * @param email Email of user attempting to login
     * @param password Provided password
     */
    public void signInWithEmailAndPassword(final String email, final String password) {
        super.mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                    if (!task.isSuccessful()) {
                        Log.w(TAG, "signInWithEmail:failed", task.getException());
//                        tryLoginMigratedUser(email, password);
                        tryLoginMigratedUser(email, password);
                    } else {
                        invalidateLoader();
                        openMain();
                    }
                });
    }

    /**
     * @param s String to be hashed
     * @return Hashed version of the string in a MD5 Hex String
     */
    @NonNull
    private static String md5(final String s) {
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

    /**
     * Method to attempt to log in a user as a migrated user. If the user is not in the list of
     * migrated users, the method will not log the user in.
     * @param email Email of the user attempting to log in
     * @param password Provided password
     */
    private void tryLoginMigratedUser(final String email, final String password) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        // Get email addresses of migrated users
        Query emailExists = ref.child("migrated_users").orderByChild("email").equalTo(email);

        emailExists.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    migratedLogin(email, password);
                } else {
                    invalidateLoader();
                    Toast.makeText(
                            LoginActivity.this,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    // TODO investigate multiple firing issue
    private void migratedLogin(final String email, final String password) {
        String secretKey = BuildConfig.MigrationSecretKey;
        super.mAuth.signInWithEmailAndPassword(email, md5(email + secretKey))
                .addOnCompleteListener(this, task -> {
                    Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful()) {
                        invalidateLoader();
                        Log.w(TAG, "signInWithEmail:failed", task.getException());
                        Toast.makeText(
                                LoginActivity.this,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            // Compare BCrypt passwords and if successful update password
                            ref.child("users").child(user.getUid()).child("bcryptPassword").addListenerForSingleValueEvent(
                                    new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {

                                            if (BCrypt.checkpw(password, dataSnapshot.getValue().toString())) {
                                                AsyncTask.execute(() -> user.updatePassword(password).addOnCompleteListener(task1 ->
                                                        ref.child("migrated_users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(
                                                                new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(DataSnapshot dataSnapshot1) {
                                                                        // Remove email address of migrated user from migrated users
                                                                        dataSnapshot1.getRef().setValue(null);
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(DatabaseError databaseError) {
                                                                        Log.w(TAG, "signInWithEmail:failed", databaseError.toException());
                                                                    }
                                                                })));

                                                invalidateLoader();
                                                openMain();
                                            } else {
                                                // Display error stating that an invalid password was used, and clear text field.
                                                Toast.makeText(LoginActivity.this, "Incorrect email or password. Please try again.", Toast.LENGTH_LONG).show();
                                                invalidateLoader();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }
                });
    }

    /**
     * Open main screen after successful login
     */
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
                Toast.makeText(
                        this,
                        "Invalid email address, please try again.",
                        Toast.LENGTH_LONG
                ).show();
                clearEditText(txtEmail);
            }
        } else {
            Toast.makeText(
                    this,
                    "Please make sure you fill in all the fields.",
                    Toast.LENGTH_LONG
            ).show();
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
        return Pattern.compile("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
                + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
                + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$").matcher(target).matches();
    }

    private void startLoader() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        txtEmail.setEnabled(false);
        txtPassword.setEnabled(false);
        txtEmail.setTextColor(Color.GRAY);
        txtPassword.setTextColor(Color.GRAY);
    }

    private void invalidateLoader() {
        loadingProgressBar.setVisibility(View.GONE);
        txtEmail.setEnabled(true);
        txtPassword.setEnabled(true);
        txtEmail.setTextColor(Color.BLACK);
        txtPassword.setTextColor(Color.BLACK);
    }
}

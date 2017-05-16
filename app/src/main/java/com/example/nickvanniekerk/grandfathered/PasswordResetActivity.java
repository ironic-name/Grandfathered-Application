package com.example.nickvanniekerk.grandfathered;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.regex.Pattern;

public class PasswordResetActivity extends AccountHelper {

    private EditText txtEmail;
    private Button btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        initializeComponents();
    }

    private void initializeComponents() {
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        btnReset = (Button) findViewById(R.id.btnReset);

        addListeners();
    }

    private void addListeners() {
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendResetEmail();
            }
        });
    }

    private void sendResetEmail() {
        String emailAddress = txtEmail.getText().toString().trim();
        if (isValidEmail(emailAddress)) {
            sendPasswordrestEmail(emailAddress);
        }
    }

    public static boolean isValidEmail(CharSequence target) {
        return Pattern.compile("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$").matcher(target).matches();
    }
}

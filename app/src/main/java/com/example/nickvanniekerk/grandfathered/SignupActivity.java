package com.example.nickvanniekerk.grandfathered;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Pattern;

public class SignupActivity extends AccountHelper {
    private EditText txtEmail;
    private EditText txtPassword;
    private EditText txtPasswordConfirm;
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeComponents();
    }

    /**
     * Called on load to assign interface variables to their respective views
     */
    private void initializeComponents() {
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        txtPassword = (EditText) findViewById(R.id.txtPassword);
        txtPasswordConfirm = (EditText) findViewById(R.id.txtPasswordConfirmation);
        btnSignUp = (Button) findViewById(R.id.btnSignUp);

        addListeners();
    }

    /**
     * Set action listeners on appropriate UI elements
     */
    private void addListeners() {
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUpClicked();
            }
        });
    }

    /**
     * Create a new Firebase user if the provided details meet the required validations.
     */
    private void signUpClicked() {
        if (validateSignUpRequirements()) {
            this.createNewUserWithEmailAndPassword(txtEmail.getText().toString().trim(), txtPassword.getText().toString().trim());
        }
    }

    /**
     * @return validity of provided information
     */
    private boolean validateSignUpRequirements() {
        Boolean valid = false;
        if (txtEmail.getText() != null || !txtEmail.getText().toString().trim().equals("") ||
                txtPassword.getText() != null || !txtPassword.getText().toString().trim().equals("") ||
                txtPasswordConfirm.getText() != null || !txtPasswordConfirm.getText().toString().trim().equals("")) {
            if (isValidEmail(txtEmail.getText().toString().trim())) {
                if (txtPasswordConfirm.getText().toString().trim().equals(txtPassword.getText().toString().trim())) {
                    valid = true;
                } else {
                    Toast.makeText(this, "Passwords don't match. Please ensure that you've entered them correctly", Toast.LENGTH_LONG).show();
                    clearEditText(txtPassword);
                    clearEditText(txtPasswordConfirm);
                }
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

package info.guardianproject.otr.app.im.app;


import info.guardianproject.cacheword.CacheWordActivityHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.otr.app.im.R;

import java.security.GeneralSecurityException;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.SherlockActivity;

public class LockScreenActivity extends SherlockActivity implements ICacheWordSubscriber {
    private static final String TAG = "LockScreenActivity";

    private final static int MIN_PASS_LENGTH = 6;
    // private final static int MAX_PASS_ATTEMPTS = 3;
    // private final static int PASS_RETRY_WAIT_TIMEOUT = 30000;

    private EditText mEnterPassphrase;
    private EditText mNewPassphrase;
    private EditText mConfirmNewPassphrase;
    private View mViewCreatePassphrase;
    private View mViewEnterPassphrase;
    private Button mBtnOpen;
    private CacheWordActivityHandler mCacheWord;
    private String mPasswordError;
    private TwoViewSlider mSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        getSherlock().getActionBar().hide();
        
        setContentView(R.layout.activity_lock_screen);
        
        mCacheWord = new CacheWordActivityHandler(this, (ICacheWordSubscriber)this);
        
        mViewCreatePassphrase = findViewById(R.id.llCreatePassphrase);
        mViewEnterPassphrase = findViewById(R.id.llEnterPassphrase);

        mEnterPassphrase = (EditText) findViewById(R.id.editEnterPassphrase);
        mNewPassphrase = (EditText) findViewById(R.id.editNewPassphrase);
        mConfirmNewPassphrase = (EditText) findViewById(R.id.editConfirmNewPassphrase);
        ViewFlipper vf = (ViewFlipper) findViewById(R.id.viewFlipper1);
        LinearLayout flipView1 = (LinearLayout) findViewById(R.id.flipView1);
        LinearLayout flipView2 = (LinearLayout) findViewById(R.id.flipView2);

        mSlider = new TwoViewSlider(vf, flipView1, flipView2, mNewPassphrase, mConfirmNewPassphrase);

    }

    @Override
    protected void onPause() {
        super.onPause();
        try { mCacheWord.onPause();}
        catch (Exception e){}
    }

    @Override
    protected void onResume() {
        super.onResume();
      
        try
        {
            mCacheWord.onResume();
        }
        catch (Exception e){}
    }

    @Override
    public void onBackPressed() {
      
        //do nothing!
    }

    private boolean newEqualsConfirmation() {
        return mNewPassphrase.getText().toString()
                .equals(mConfirmNewPassphrase.getText().toString());
    }

    private void showValidationError() {
        Toast.makeText(LockScreenActivity.this, mPasswordError, Toast.LENGTH_LONG).show();
        mNewPassphrase.requestFocus();
    }

    private void showInequalityError() {
        Toast.makeText(LockScreenActivity.this,
                getString(R.string.lock_screen_passphrases_not_matching),
                Toast.LENGTH_SHORT).show();
        clearNewFields();
    }

    private void clearNewFields() {
        mNewPassphrase.getEditableText().clear();
        mConfirmNewPassphrase.getEditableText().clear();
    }

    private boolean isPasswordValid() {
        return validatePassword(mNewPassphrase.getText().toString().toCharArray());
    }

    private boolean isConfirmationFieldEmpty() {
        return mConfirmNewPassphrase.getText().toString().length() == 0;
    }

    private void initializePassphrase() {
        // Passphrase is not set, so allow the user to create one

        View viewCreatePassphrase = findViewById(R.id.llCreatePassphrase);
        viewCreatePassphrase.setVisibility(View.VISIBLE);
        mViewEnterPassphrase.setVisibility(View.GONE);

        mNewPassphrase.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE)
                {
                    if (!isPasswordValid())
                        showValidationError();
                    else
                        mSlider.showConfirmationField();
                }
                return false;
            }
        });

        mConfirmNewPassphrase.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE)
                {
                    if (!newEqualsConfirmation()) {
                        showInequalityError();
                        mSlider.showNewPasswordField();
                    }
                }
                return false;
            }
        });

        Button btnCreate = (Button) findViewById(R.id.btnCreate);
        btnCreate.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                // validate
                if (!isPasswordValid()) {
                    showValidationError();
                    mSlider.showNewPasswordField();
                } else if (isConfirmationFieldEmpty()) {
                    mSlider.showConfirmationField();
                } else if (!newEqualsConfirmation()) {
                    showInequalityError();
                    mSlider.showNewPasswordField();
                } else {
                    try {
                        mCacheWord.setPassphrase(mNewPassphrase.getText().toString().toCharArray());
                    } catch (GeneralSecurityException e) {
                        // TODO initialization failed
                        Log.e(TAG, "Cacheword pass initialization failed: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void promptPassphrase() {
        mViewCreatePassphrase.setVisibility(View.GONE);
        mViewEnterPassphrase.setVisibility(View.VISIBLE);

        mBtnOpen = (Button) findViewById(R.id.btnOpen);
        mBtnOpen.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                if (mEnterPassphrase.getText().toString().length() == 0)
                    return;
                // Check passphrase
                try {
                    mCacheWord.setPassphrase(mEnterPassphrase.getText().toString().toCharArray());
                } catch (GeneralSecurityException e) {
                    mEnterPassphrase.setText("");
                    // TODO implement try again and wipe if fail
                    Log.e(TAG, "Cacheword pass verification failed: " + e.getMessage());
                    return;
                }
            }
        });

        mEnterPassphrase.setOnEditorActionListener(new OnEditorActionListener()
        {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_GO)
                {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    Handler threadHandler = new Handler();
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0, new ResultReceiver(
                            threadHandler)
                    {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData)
                        {
                            super.onReceiveResult(resultCode, resultData);
                            mBtnOpen.performClick();
                        }
                    });
                    return true;
                }
                return false;
            }
        });
    }

    private boolean validatePassword(char[] pass)
    {
        boolean upper = false;
        boolean lower = false;
        boolean number = false;
        for (char c : pass) {
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                number = true;
            }
        }

        if (pass.length < MIN_PASS_LENGTH)
        {
            // should we support some user string message here?
            mPasswordError = getString(R.string.pass_err_length);
            return false;
        }
        //only enforce password length
        /*
        else if (!upper)
        {
            mPasswordError = getString(R.string.pass_err_upper);
            return false;
        }
        else if (!lower)
        {
            mPasswordError = getString(R.string.pass_err_lower);
            return false;
        }
        else if (!number)
        {
            mPasswordError = getString(R.string.pass_err_num);
            return false;
        }*/
        // if it got here, then must be okay
        // hopefully the user can remember it
        return true;
    }

    public class TwoViewSlider {

        private boolean firstIsShown = true;
        private ViewFlipper flipper;
        private LinearLayout container1;
        private LinearLayout container2;
        private View firstView;
        private View secondView;
        private Animation pushRightIn;
        private Animation pushRightOut;
        private Animation pushLeftIn;
        private Animation pushLeftOut;

        public TwoViewSlider(ViewFlipper flipper, LinearLayout container1, LinearLayout container2,
                View view1, View view2) {
            this.flipper = flipper;
            this.container1 = container1;
            this.container2 = container2;
            this.firstView = view1;
            this.secondView = view2;

            pushRightIn = AnimationUtils.loadAnimation(LockScreenActivity.this, R.anim.push_right_in);
            pushRightOut = AnimationUtils.loadAnimation(LockScreenActivity.this, R.anim.push_right_out);
            pushLeftIn = AnimationUtils.loadAnimation(LockScreenActivity.this, R.anim.push_left_in);
            pushLeftOut = AnimationUtils.loadAnimation(LockScreenActivity.this, R.anim.push_left_out);

        }

        public void showNewPasswordField() {
            if (firstIsShown)
                return;

            flipper.setInAnimation(pushRightIn);
            flipper.setOutAnimation(pushRightOut);
            flip();
        }

        public void showConfirmationField() {
            if (!firstIsShown)
                return;

            flipper.setInAnimation(pushLeftIn);
            flipper.setOutAnimation(pushLeftOut);
            flip();
        }

        private void flip() {
            if (firstIsShown) {
                firstIsShown = false;
                container2.removeAllViews();
                container2.addView(secondView);
            } else {
                firstIsShown = true;
                container1.removeAllViews();
                container1.addView(firstView);
            }
            flipper.showNext();
        }
    }

    @Override
    public void onCacheWordUninitialized() {
        initializePassphrase();
        
    }

    @Override
    public void onCacheWordLocked() {
        promptPassphrase();
        
    }

    @Override
    public void onCacheWordOpened() {
        Intent intent = (Intent) getIntent().getParcelableExtra("originalIntent");
        
        if (intent != null)
        {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);
            finish();
            LockScreenActivity.this.overridePendingTransition(0, 0);
        }
    }
    

}
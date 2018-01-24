package info.guardianproject.otr.app.im.app;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import info.guardianproject.cacheword.CacheWordActivityHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.BackgroundBitmapLoaderTask;
import info.guardianproject.util.Languages;

import java.security.GeneralSecurityException;

public class LockScreenActivity extends ThemeableActivity implements ICacheWordSubscriber {
    private static final String TAG = "LockScreenActivity";

    private final static int MIN_PASS_LENGTH = 4;
    // private final static int MAX_PASS_ATTEMPTS = 3;
    // private final static int PASS_RETRY_WAIT_TIMEOUT = 30000;

    private EditText mEnterPassphrase;
    private EditText mNewPassphrase;
    private EditText mConfirmNewPassphrase;
    private View mViewCreatePassphrase;
    private View mViewEnterPassphrase;
    private ImageButton mLanguageButton;

    private CacheWordActivityHandler mCacheWord;
    private String mPasswordError;
    private TwoViewSlider mSlider;

    private ImApp mApp;
    private Button mBtnCreate;
    private Button mBtnSkip;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mApp = (ImApp)getApplication();

        getSupportActionBar().hide();

        setContentView(R.layout.activity_lock_screen);

        mCacheWord = new CacheWordActivityHandler(mApp, (ICacheWordSubscriber)this);

        mViewCreatePassphrase = findViewById(R.id.llCreatePassphrase);
        mViewEnterPassphrase = findViewById(R.id.llEnterPassphrase);

        mEnterPassphrase = (EditText) findViewById(R.id.editEnterPassphrase);

        mNewPassphrase = (EditText) findViewById(R.id.editNewPassphrase);
        mConfirmNewPassphrase = (EditText) findViewById(R.id.editConfirmNewPassphrase);
        ViewFlipper vf = (ViewFlipper) findViewById(R.id.viewFlipper1);
        LinearLayout flipView1 = (LinearLayout) findViewById(R.id.flipView1);
        LinearLayout flipView2 = (LinearLayout) findViewById(R.id.flipView2);

        mSlider = new TwoViewSlider(vf, flipView1, flipView2, mNewPassphrase, mConfirmNewPassphrase);

        // set up language chooser button
        mLanguageButton = (ImageButton) findViewById(R.id.languageButton);
        mLanguageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = LockScreenActivity.this;
                final Languages languages = Languages.get(activity);
                final ArrayAdapter<String> languagesAdapter = new ArrayAdapter<String>(activity,
                        android.R.layout.simple_list_item_single_choice, languages.getAllNames());
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setIcon(R.drawable.ic_settings_language);
                builder.setTitle(R.string.KEY_PREF_LANGUAGE_TITLE);
                builder.setAdapter(languagesAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        Log.i(TAG, "onItemClick: " + dialog + " " + position);
                        String[] languageCodes = languages.getSupportedLocales();
                        resetLanguage(languageCodes[position]);
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mCacheWord.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCacheWord.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCacheWord.disconnect();
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

    private boolean isPasswordFieldEmpty() {
        return mNewPassphrase.getText().toString().length() == 0;
    }

    private boolean isConfirmationFieldEmpty() {
        return mConfirmNewPassphrase.getText().toString().length() == 0;
    }

    private void initializeWithPassphrase() {
        try {
            String passphrase = mNewPassphrase.getText().toString();
            if (passphrase.isEmpty()) {
                // Create DB with empty passphrase
                if (Imps.setEmptyPassphrase(this, false)) {
                    
                    try
                    {
                        ChatFileStore.initWithoutPassword(this);

                        // Simulate cacheword opening
                        afterCacheWordOpened();
                    }
                    catch (Exception e)
                    {
                        Log.d(ImApp.LOG_TAG,"unable to mount VFS store"); //but let's not crash the whole app right now
                    }
                    
                    ChatFileStore.initWithoutPassword(this);
                    
                }  else {
                    // TODO failed
                }
            } else {
                mCacheWord.setPassphrase(passphrase.toCharArray());
            }
        } catch (Exception e) {
            // TODO initialization failed
            Log.e(TAG, "Cacheword pass initialization failed: " + e.getMessage());
        }
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
                    else if (isPasswordFieldEmpty()) {
                        initializeWithPassphrase();
                    } else
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

        mBtnCreate = (Button) findViewById(R.id.btnCreate);
        mBtnCreate.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // validate
                if (!isPasswordValid()) {
                    showValidationError();
                    mSlider.showNewPasswordField();
                } else if (isConfirmationFieldEmpty() && !isPasswordFieldEmpty()) {
                    mBtnSkip.setVisibility(View.GONE);
                    mSlider.showConfirmationField();
                    mBtnCreate.setText(R.string.lock_screen_confirm_passphrase);
                }
                else if (!newEqualsConfirmation()) {
                    showInequalityError();
                }
               else if (!isConfirmationFieldEmpty() && !isPasswordFieldEmpty())
               {
                    initializeWithPassphrase();
                }
            }
        });



        mBtnSkip = (Button)findViewById(R.id.btnSkip);
        mBtnSkip.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v)
            {
                if (isPasswordFieldEmpty())
                    initializeWithPassphrase();

            }
        });
    }

    private void promptPassphrase() {
        mViewCreatePassphrase.setVisibility(View.GONE);
        mViewEnterPassphrase.setVisibility(View.VISIBLE);

        mEnterPassphrase.setOnEditorActionListener(new OnEditorActionListener()
        {
            @Override
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

                            if (mEnterPassphrase.getText().toString().length() == 0)
                                return;
                            // Check passphrase
                            try {
                                char[] passphrase = mEnterPassphrase.getText().toString().toCharArray();

                                mCacheWord.setPassphrase(passphrase);
                                ImApp.mUsingCacheword = true;
                            } catch (Exception e) {
                                mEnterPassphrase.setText("");
                                // TODO implement try again and wipe if fail
                                Log.e(TAG, "Cacheword pass verification failed: " + e.getMessage());
                                return;
                            }

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

        if (pass.length < MIN_PASS_LENGTH && pass.length != 0)
        {
            // should we support some user string message here?
            mPasswordError = getString(R.string.pass_err_length);
            return false;
        }

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

        Intent intentOrig;

        if ((intentOrig = getIntent().getParcelableExtra("originalIntent"))!=null)
        {
            if (intentOrig.getData() != null)
            {
                if (intentOrig.getData().getScheme().equals("immu")||
                intentOrig.getData().getScheme().equals("ima"))
                {

                    initializeWithPassphrase();
                    return;
                }
            }
        }


        initializePassphrase();

    }

    @Override
    public void onCacheWordLocked() {
        promptPassphrase();
    }

    @Override
    public void onCacheWordOpened() {
        afterCacheWordOpened();
        ChatFileStore.init(this, mCacheWord.getEncryptionKey());
    }

    /**
     *
     */
    private void afterCacheWordOpened() {
        Intent intent = (Intent) getIntent().getParcelableExtra("originalIntent");

        if (intent != null)
        {

            getIntent().removeExtra("originalIntent");
            finish();
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
           // LockScreenActivity.this.overridePendingTransition(0, 0);
        }
    }

    private void resetLanguage(String language) {
        ((ImApp) getApplication()).setNewLocale(this, language);
        Intent intent = getIntent();
        intent.setClass(LockScreenActivity.this, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
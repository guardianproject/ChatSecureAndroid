/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.IOtrChatSession;
import info.guardianproject.otr.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.OtrChatManager;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IContactList;
import info.guardianproject.otr.app.im.IContactListListener;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.ISubscriptionListener;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ContactListFilterView.ContactListListener;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.plugin.xmpp.XmppAddress;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.otr.app.im.ui.SecureCameraActivity;
import info.guardianproject.util.LogCleaner;
import info.guardianproject.util.SystemServices;
import info.guardianproject.util.SystemServices.FileInfo;
import info.guardianproject.util.XmppUriHelper;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionStatus;

import org.ironrabbit.type.CustomTypefaceManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class NewChatActivity extends FragmentActivity implements View.OnCreateContextMenuListener {

    private static final String ICICLE_CHAT_PAGER_ADAPTER = "chatPagerAdapter";
    private static final String ICICLE_POSITION = "position";
    private static final int MENU_RESEND = Menu.FIRST;

    private static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;
    private static final int REQUEST_SEND_IMAGE = REQUEST_PICK_CONTACTS + 1;
    private static final int REQUEST_SEND_FILE = REQUEST_SEND_IMAGE + 1;
    private static final int REQUEST_SEND_AUDIO = REQUEST_SEND_FILE + 1;
    private static final int REQUEST_TAKE_PICTURE = REQUEST_SEND_AUDIO + 1;
    private static final int REQUEST_SETTINGS = REQUEST_TAKE_PICTURE + 1;
    private static final int REQUEST_TAKE_PICTURE_SECURE = REQUEST_SETTINGS + 1;

    private static final int CONTACT_LIST_LOADER_ID = 4444;
    private static final int CHAT_LIST_LOADER_ID = CONTACT_LIST_LOADER_ID+1;
    private static final int CHAT_PAGE_LOADER_ID = CONTACT_LIST_LOADER_ID+2;

    private ImApp mApp;
    private ViewPager mChatPager;
    private ChatViewPagerAdapter mChatPagerAdapter;

    private Toolbar mToolbar;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    private int mLastPagePosition = -1;

    private SimpleAlertHandler mHandler;

    private long mLastAccountId = -1;
    private long mLastProviderId = -1;
    private boolean mShowChatsOnly = true;

    private MessageContextMenuHandler mMessageContextMenuHandler;

    private ContactListFragment mContactList = null;

    private Imps.ProviderSettings.QueryMap mGlobalSettings = null;

    final static class MyHandler extends SimpleAlertHandler {
        public MyHandler(NewChatActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ImApp.EVENT_SERVICE_CONNECTED) {
                ((NewChatActivity)mActivity).onServiceConnected();
                return;
            }
            super.handleMessage(msg);
        }
    }


    @Override
    protected void onCreate(Bundle icicle) {

        super.onCreate(icicle);

        checkCustomFont ();

        mApp = (ImApp)getApplication();
        mApp.maybeInit(this);

        setContentView(R.layout.chat_pager);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mApp.setAppTheme(this,mToolbar);
        ThemeableActivity.setBackgroundImage(this);

        mToolbar.inflateMenu(R.menu.chat_screen_menu);
        setupMenu ();

        setTitle(R.string.app_name);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
            this,  mDrawer, mToolbar,
            R.string.ok, R.string.cancel
        );
        // Set the drawer toggle as the DrawerListener
        mDrawer.setDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerToggle.syncState();
        mDrawerToggle.setToolbarNavigationClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {

                int currentPos = mChatPager.getCurrentItem();
                if (currentPos > 0) {
                    mChatPager.setCurrentItem(0);

                }

            }


        });

        mHandler = new MyHandler(this);
        mRequestedChatId = -1;

        mChatPager = (ViewPager) findViewById(R.id.chatpager);
        //mChatPager.setSaveEnabled(false);
        //mChatPager.setOffscreenPageLimit(3);
        //mChatPager.setDrawingCacheEnabled(true);
        //mChatPager.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        mChatPager.setOnPageChangeListener(new SimpleOnPageChangeListener () {

            @Override
            public void onPageSelected(int pos) {

                if (pos > 0) {

                    if (mLastPagePosition != -1)
                    {
                        ChatViewFragment frag = (ChatViewFragment)mChatPagerAdapter.getItemAt(pos);
                        // Fragment isn't guaranteed to be initialized yet
                        if (frag != null)
                            frag.onDeselected(mApp);
                    }

                    ChatViewFragment frag = (ChatViewFragment)mChatPagerAdapter.getItemAt(pos);
                    // Fragment isn't guaranteed to be initialized yet
                    if (frag != null)
                        frag.onSelected(mApp);

                    mLastPagePosition = pos;

                    if (mMenu != null)
                    {

                        mMenu.setGroupVisible(R.id.menu_group_chats, true);
                        mMenu.setGroupVisible(R.id.menu_group_contacts, false);

                    }

                    mDrawerToggle.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
                    mDrawerToggle.setDrawerIndicatorEnabled(false);
                    mDrawerToggle.syncState();


                }
                else
                {
                    if (mMenu != null)
                    {
                        mMenu.setGroupVisible(R.id.menu_group_chats, false);
                        mMenu.setGroupVisible(R.id.menu_group_contacts, true);

                        mMenu.setGroupVisible(R.id.menu_group_otr_verified,false);
                        mMenu.setGroupVisible(R.id.menu_group_otr_unverified,false);
                        mMenu.setGroupVisible(R.id.menu_group_otr_off,false);

                    }

                    setTitle(R.string.app_name);
                    mDrawerToggle.setHomeAsUpIndicator(null);
                    mDrawerToggle.setDrawerIndicatorEnabled(true);
                    mDrawerToggle.syncState();

                }

            }



        });

        mMessageContextMenuHandler = new MessageContextMenuHandler();

       // initSideBar ();

        mChatPagerAdapter = new ChatViewPagerAdapter(getSupportFragmentManager());
        mChatPager.setAdapter(mChatPagerAdapter);

        if (icicle != null) {
            if (icicle.containsKey(ICICLE_CHAT_PAGER_ADAPTER)) {
                mChatPagerAdapter.restoreState(icicle.getParcelable(ICICLE_CHAT_PAGER_ADAPTER), getClassLoader());
            }
            if (icicle.containsKey(ICICLE_POSITION)) {
                int position = icicle.getInt(ICICLE_POSITION);
                if (position < mChatPagerAdapter.getCount())
                    mChatPager.setCurrentItem(position);
            }
        }

        mApp.registerForBroadcastEvent(ImApp.EVENT_SERVICE_CONNECTED, mHandler);

        initConnections();

    }
    
    private void initChats ()
    {
        LoaderManager lMgr =getSupportLoaderManager();
        lMgr.initLoader(CHAT_LIST_LOADER_ID, null, new LoaderManager.LoaderCallbacks<Cursor> () {

            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                CursorLoader loader = new CursorLoader(NewChatActivity.this, Imps.Contacts.CONTENT_URI_CHAT_CONTACTS, ChatView.CHAT_PROJECTION, null, null, null);
                loader.setUpdateThrottle(100L);

                return loader;
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {

                mChatPagerAdapter.swapCursor(newCursor);
                updateChatList();

                if (getIntent() != null)
                    resolveIntent(getIntent());

                if (mRequestedChatId >= 0) {
                    if (showChat(mRequestedChatId)) {
                        mRequestedChatId = -1;
                    }
                }


            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mChatPagerAdapter.swapCursor(null);
            }
        });
    }



    @Override
    public void setTitle(CharSequence title) {

        mToolbar.setTitle(title);
      //  mToolbar.setLogo(null);
    }

    public void setTitle(CharSequence title, Drawable icon) {

        mToolbar.setTitle(title);
     //   mToolbar.setLogo(icon);
    }


    private void checkCustomFont ()
    {
        if (CustomTypefaceManager.getCurrentTypeface(this)==null)
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();

            final int N = mInputMethodProperties.size();

            for (int i = 0; i < N; i++) {

                InputMethodInfo imi = mInputMethodProperties.get(i);

                //imi contains the information about the keyboard you are using
                if (imi.getPackageName().equals("org.ironrabbit.bhoboard"))
                {
                    CustomTypefaceManager.loadFromKeyboard(this);   
                    break;
                }
            
            }
            

        }
    }

    /*
     * We must have been thawed and the service was not previously connected, so our ChatViews are showing nothing.
     * Refresh them.
     */
    void onServiceConnected() {
        if (mChatPagerAdapter != null) {
            int size = mChatPagerAdapter.getCount();
            for (int i = 1; i < size ; i++) {
                ChatViewFragment frag = (ChatViewFragment)mChatPagerAdapter.getItemAt(i);
                if (frag != null) {
                    frag.onServiceConnected();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterSubListeners ();

        if (mGlobalSettings != null)
        {
            mGlobalSettings.close();
            mGlobalSettings = null;
        }

        mApp.unregisterForBroadcastEvent(ImApp.EVENT_SERVICE_CONNECTED, mHandler);
        mChatPagerAdapter.swapCursor(null);
        //mAdapter.swapCursor(null);
        super.onDestroy();
        mChatPagerAdapter = null;
       // mAdapter = null;


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ICICLE_POSITION, mChatPager.getCurrentItem());
        outState.putParcelable(ICICLE_CHAT_PAGER_ADAPTER, mChatPagerAdapter.saveState());
    }

    @Override
    protected void onResume() {
        super.onResume();

        mApp.getTrustManager().bindDisplayActivity(this);

        mApp.checkForCrashes(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mApp.getTrustManager().unbindDisplayActivity(this);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        resolveIntent(intent);
    }

    @Override
    public void onBackPressed() {

        int currentPos = mChatPager.getCurrentItem();
        if (currentPos > 0) {
            mChatPager.setCurrentItem(0);
            return;
        }

        super.onBackPressed();
    }

    private void showInviteContactDialog ()
    {

        Intent i = new Intent(this, AddContactActivity.class);
        startActivity(i);

    }

    private IOtrChatSession getCurrentOtrChatSession() throws RemoteException {
        IChatSession chatSession = getCurrentChatSession();
        if (chatSession == null)
            return null;
        else
            return chatSession.getOtrChatSession();
    }

    private void displayQRCode ()
    {
        try
        {
            IOtrChatSession iOtr = getCurrentOtrChatSession();
            if (iOtr != null)
            {
                String localUser = iOtr.getLocalUserId();
                String localFingerprint = iOtr.getLocalFingerprint();

                if (localFingerprint != null)
                {
                    String otrKeyURI = XmppUriHelper.getUri(localUser, localFingerprint);

                    new IntentIntegrator(this).shareText(otrKeyURI);
                    return;
                 }
            }
        }
        catch (RemoteException re)
        {
        }

        //did not work
        Toast.makeText(this, R.string.please_start_a_secure_conversation_before_scanning_codes, Toast.LENGTH_LONG).show();
     }

    private void resolveIntent(Intent intent) {

        doResolveIntent(intent);
        setIntent(null);

    }

    private IImConnection findConnectionForGroupChat (String user, String host)
    {
        Collection<IImConnection> connActive = mApp.getActiveConnections();
        ContentResolver cr = this.getContentResolver();
        IImConnection result = null;

        for (IImConnection conn : connActive)
        {
            try
            {

                    Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString( conn.getProviderId())},null);

                    Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(pCursor, cr,
                            conn.getProviderId(),     false /* keep updated */, mHandler /* no handler */);

                    if (host.contains(settings.getDomain()))
                    {
                        if (conn.getState() == ImConnection.LOGGED_IN)
                        {

                            result = conn;
                            settings.close();
                            pCursor.close();
                            break;
                        }
                    }

                    settings.close();
                    pCursor.close();

            }
            catch (RemoteException e){//nothing to do here
            }

        }

        return result;
    }

    private void doResolveIntent(Intent intent) {

        if (requireOpenDashboardOnStart(intent)) {
            long providerId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, -1L);
            mLastAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID,
                    -1L);
            if (providerId == -1L || mLastAccountId == -1L) {
                finish();
            } else {
             //   mChatSwitcher.open();
            }
            return;
        }

        if (ImServiceConstants.ACTION_MANAGE_SUBSCRIPTION.equals(intent.getAction())) {

            long providerId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, -1);
            mLastAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID,
                    -1L);
            String from = intent.getStringExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS);

            if ((providerId == -1) || (from == null)) {
                finish();
            } else {

                showSubscriptionDialog (providerId, from);

            }
        } else if (intent != null) {
            Uri data = intent.getData();

           if (intent.getBooleanExtra("showaccounts", false))
               mDrawer.openDrawer(GravityCompat.START);

            if (data != null)
            {
                if (data.getScheme() != null && data.getScheme().equals("immu"))
                {
                    String user = data.getUserInfo();
                    String host = data.getHost();
                    String path = null;

                    if (data.getPathSegments().size() > 0)
                        path = data.getPathSegments().get(0);

                    if (host != null && path != null)
                    {

                        IImConnection connMUC = findConnectionForGroupChat(user, host);

                        if (connMUC != null)
                        {

                            startGroupChat (path, host, user, connMUC);
                            setResult(RESULT_OK);
                        }
                        else
                        {
                            mHandler.showAlert("Connection Error", "Unable to find a connection to join a group chat from. Please sign in and try again.");
                            setResult(Activity.RESULT_CANCELED);
                            finish();
                        }

                     }



                } else {

                    String type = getContentResolver().getType(data);
                    if (Imps.Chats.CONTENT_ITEM_TYPE.equals(type)) {

                        long requestedContactId = ContentUris.parseId(data);

                        Cursor cursorChats = mChatPagerAdapter.getCursor();

                        if (cursorChats != null)
                        {
                            cursorChats.moveToPosition(-1);
                            int posIdx = 1;
                            boolean foundChatView = false;

                            while (cursorChats.moveToNext())
                            {
                                long chatId = cursorChats.getLong(ChatView.CONTACT_ID_COLUMN);

                                if (chatId == requestedContactId)
                                {
                                    mChatPager.setCurrentItem(posIdx);
                                    foundChatView = true;
                                    break;
                                }

                                posIdx++;
                            }

                            if (!foundChatView)
                            {

                                Uri.Builder builder = Imps.Contacts.CONTENT_URI.buildUpon();
                                ContentUris.appendId(builder, requestedContactId);
                                Cursor cursor = getContentResolver().query(builder.build(), ChatView.CHAT_PROJECTION, null, null, null);

                                try {
                                    if (cursor.getCount() > 0)
                                    {
                                        cursor.moveToFirst();
                                        openExistingChat(cursor);
                                    }
                                } finally {
                                    cursor.close();
                                }
                            }
                        }

                    } else if (Imps.Invitation.CONTENT_ITEM_TYPE.equals(type)) {
                        //chatView.bindInvitation(ContentUris.parseId(data));




                    }
                }
            }
            else if (intent.hasExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID))
            {
                //set the current account id
                mLastAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID,-1L);

                //move the pager back to the first page
                if (mChatPager != null)
                    mChatPager.setCurrentItem(0);



            }
            else
            {
              //  refreshConnections();
            }
        }


    }

    public boolean showChat (long requestedChatId)
    {
        Cursor cursorChats = mChatPagerAdapter.getCursor();
        
        if (cursorChats == null)
            return false;
        
        cursorChats.moveToPosition(-1);
        int posIdx = 1;

        while (cursorChats.moveToNext())
        {
            long chatId = cursorChats.getLong(ChatView.CONTACT_ID_COLUMN);

            if (chatId == requestedChatId)
            {
                mChatPager.setCurrentItem(posIdx);
                return true;
            }

            posIdx++;
        }

        // Was not found
        return false;
    }

    public void refreshChatViews ()
    {
        mChatPagerAdapter.notifyDataSetChanged();
        
    }

    private Menu mMenu;
   // private AccountAdapter mAdapter;
    protected Long[][] mAccountIds;
    private long mRequestedChatId;

    public void updateEncryptionMenuState ()
    {
        ChatView cView = getCurrentChatView();

        if (cView == null)
            return;

        if (mChatPager != null && mMenu != null)
        {
            if (mChatPager.getCurrentItem() > 0)
            {
                // phoenix-nz - a group chat should not be shown as 'unverified' as it (currently)
                // cannot be verified. Thus, show as neutral.
                if (cView.isGroupChat())
                {
                    mMenu.setGroupVisible(R.id.menu_group_otr_verified,false);
                    mMenu.setGroupVisible(R.id.menu_group_otr_unverified,false);
                    mMenu.setGroupVisible(R.id.menu_group_otr_off,false);

                }
                else if (cView.getOtrSessionStatus() == SessionStatus.ENCRYPTED && cView.isOtrSessionVerified())
                {
                    mMenu.setGroupVisible(R.id.menu_group_otr_verified,true);
                    mMenu.setGroupVisible(R.id.menu_group_otr_unverified,false);
                    mMenu.setGroupVisible(R.id.menu_group_otr_off,false);

                }
                else if (cView.getOtrSessionStatus() == SessionStatus.ENCRYPTED)
                {
                    mMenu.setGroupVisible(R.id.menu_group_otr_unverified,true);
                    mMenu.setGroupVisible(R.id.menu_group_otr_verified,false);
                    mMenu.setGroupVisible(R.id.menu_group_otr_off,false);



                }
                else if (cView.getOtrSessionStatus() == SessionStatus.FINISHED)
                {
                    mMenu.setGroupVisible(R.id.menu_group_otr_unverified,true);
                    mMenu.setGroupVisible(R.id.menu_group_otr_verified,false);
                    mMenu.setGroupVisible(R.id.menu_group_otr_off,false);

                }
                else
                {
                    mMenu.setGroupVisible(R.id.menu_group_otr_off,true);

                    mMenu.setGroupVisible(R.id.menu_group_otr_verified,false);
                    mMenu.setGroupVisible(R.id.menu_group_otr_unverified,false);

                }

            }

         }
    }

    private void setupMenu ()
    {

        mMenu = mToolbar.getMenu();

        if (mMenu != null)
        {
            if (mChatPager != null && mChatPager.getCurrentItem() > 0)
            {
                mMenu.setGroupVisible(R.id.menu_group_chats, true);
                mMenu.setGroupVisible(R.id.menu_group_contacts, false);

            }
            else
            {
                mMenu.setGroupVisible(R.id.menu_group_chats, false);
                mMenu.setGroupVisible(R.id.menu_group_contacts, true);

                mMenu.setGroupVisible(R.id.menu_group_otr_verified,false);
                mMenu.setGroupVisible(R.id.menu_group_otr_unverified,false);
                mMenu.setGroupVisible(R.id.menu_group_otr_off,false);

            }
        }

        mToolbar.setOnMenuItemClickListener(new OnMenuItemClickListener ()
        {

            @Override
            public boolean onMenuItemClick(MenuItem item) {

                
                switch (item.getItemId()) {
/*
                case R.id.menu_send_image:
                    if (getCurrentChatView() != null && getCurrentChatView().getOtrSessionStatus() == SessionStatus.ENCRYPTED)
                    {
                       startImagePicker();
                    }
                    else
                    {
                        mHandler.showServiceErrorAlert(getString(R.string.please_enable_chat_encryption_to_share_files));
                    }
                    return true;
                case R.id.menu_take_picture:
                    if (getCurrentChatView() != null && getCurrentChatView().getOtrSessionStatus() == SessionStatus.ENCRYPTED)
                    {
                        startPhotoTaker();
                    }
                    else
                    {
                        mHandler.showServiceErrorAlert(getString(R.string.please_enable_chat_encryption_to_share_files));
                    }
                    return true;

                case R.id.menu_send_file:

                    if (getCurrentChatView() != null && getCurrentChatView().getOtrSessionStatus() == SessionStatus.ENCRYPTED)
                    {
                       startFilePicker();
                    }
                    else
                    {
                        mHandler.showServiceErrorAlert(getString(R.string.please_enable_chat_encryption_to_share_files));
                    }

                    return true;

                case R.id.menu_send_audio:

                    if (getCurrentChatView() != null && getCurrentChatView().getOtrSessionStatus() == SessionStatus.ENCRYPTED)
                    {
                       startAudioPicker();
                    }
                    else
                    {
                        mHandler.showServiceErrorAlert(getString(R.string.please_enable_chat_encryption_to_share_files));
                    }

                    return true;
*/
                case R.id.menu_verify_or_view:
                    if (getCurrentChatView() != null)
                        getCurrentChatView().showVerifyDialog();
                    return true;

                case R.id.menu_show_qr:
                    displayQRCode();
                    return true;
                case R.id.menu_end_conversation:
                    try {
                        endCurrentChatPrompt( getCurrentSessionId());
                    } catch (Exception e) {
                        Toast.makeText(NewChatActivity.this, "Error:" + e.getMessage(), Toast.LENGTH_LONG).show(); // TODO i18n
                        e.printStackTrace();
                    }

                    return true;
                /*
                case R.id.menu_delete_conversation:
                    if (getCurrentChatView() != null)
                        getCurrentChatView().closeChatSession(true);
                    return true;
                  */
                case R.id.menu_settings:
                    Intent sintent = new Intent(NewChatActivity.this, SettingActivity.class);
                    startActivityForResult(sintent,REQUEST_SETTINGS);
                    return true;

                case R.id.menu_otr:
                case R.id.menu_otr_stop:
                case R.id.menu_otr_stop_verified:
                case R.id.menu_view_profile_verified:
                    
                    if (getCurrentChatView() != null)
                    {

                        boolean isEnc = (getCurrentChatView().getOtrSessionStatus() == SessionStatus.ENCRYPTED ||
                                getCurrentChatView().getOtrSessionStatus() == SessionStatus.FINISHED
                                );

                        getCurrentChatView().setOTRState(!isEnc);



                    }

                    return true;

                case android.R.id.home:
                    int currentPos = mChatPager.getCurrentItem();
                    if (currentPos > 0) {
                        mChatPager.setCurrentItem(0);

                    }

                    return true;

                case R.id.menu_view_accounts:
                    startActivity(new Intent(getBaseContext(), ChooseAccountActivity.class));

                    return true;

                case R.id.menu_new_chat:
                    startContactPicker();
                    return true;

                case R.id.menu_exit:
                    doHardShutdown();
                    return true;

                case R.id.menu_add_contact:
                    showInviteContactDialog();
                    return true;

                case R.id.menu_group_chat:
                    showGroupChatDialog();
                    return true;
                case R.id.menu_import_keys:
                    importKeyStore();
                    return true;
                }

                return false;

            }

        });


    }


    private void importKeyStore ()
    {
        boolean doKeyStoreImport = OtrAndroidKeyManagerImpl.checkForKeyImport(getIntent(), this);

    }

    private void exportKeyStore ()
    {
        //boolean doKeyStoreExport = OtrAndroidKeyManagerImpl.getInstance(this).doKeyStoreExport(password);

    }

    private void endCurrentChatPrompt( final String sessionId ) {
        OtrChatManager otrChatManager = OtrChatManager.getInstance();
        if (otrChatManager != null) {
            try {
                IOtrChatSession otrSession = getCurrentOtrChatSession();
                otrSession.stopChatEncryption();
            } catch (RemoteException e) {
            }
        }
        // if no files to delete - just end the session
        if (!IocVfs.sessionExists(sessionId)) {
            endCurrentChat();
            return;
        }
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(getString(R.string.end_chat_title))
        .setMessage(getString(R.string.end_chat_summary))
        .setPositiveButton(getString(R.string.end_chat_and_delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                endCurrentChat();
            }
        })
        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        })
        .show();
    }

    private void endCurrentChat()
    {
        if (getCurrentChatView() != null) {
            try {
                // delete the chat session's files if any
                deleteSessionVfs( getCurrentSessionId() );
            } catch (Exception e) {
                // TODO error
                e.printStackTrace();
            }
            getCurrentChatView().closeChatSession(true);
        }
        
        updateChatList();
        mChatPager.setCurrentItem(0);
        
    }

    private void deleteSessionVfs( final String sessionId ) throws Exception {
        // if no files to delete - bail
        if (!IocVfs.sessionExists(sessionId)) {
            return;
        }
        // delete
        IocVfs.deleteSession(sessionId);
    }

    private void startContactPicker() {

        Uri.Builder builder = Imps.Contacts.CONTENT_URI_CONTACTS_BY.buildUpon();
        Uri data = builder.build();
        
        Intent i = new Intent(Intent.ACTION_PICK, data);
        i.putExtra("invitations", false);
        startActivityForResult(i, REQUEST_PICK_CONTACTS);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        /*
         * this behavior doesn't make sense to me. i hit the back key
         * and the chat disappears, as opposed to just going back to the message
         * list. i think the user should have it to use the 'end chat' button to really clear a chat
         * n8fr8 2012/09/26
         *
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
            && event.getAction() == KeyEvent.ACTION_DOWN) {
            mChatView.closeChatSessionIfInactive();
        }*/

        return super.dispatchKeyEvent(event);
    }

    /** Check whether we are asked to open Dashboard on startup. */
    private boolean requireOpenDashboardOnStart(Intent intent) {
        return intent.getBooleanExtra(ImServiceConstants.EXTRA_INTENT_SHOW_MULTIPLE, false);
    }

    void startImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SEND_IMAGE);
    }

    Uri mLastPhoto = null;

    void startPhotoTaker() {

        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),  "cs_" + new Date().getTime() + ".jpg");
        mLastPhoto = Uri.fromFile(photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                mLastPhoto);

        // start the image capture Intent
        startActivityForResult(intent, REQUEST_TAKE_PICTURE);
    }

    void startPhotoTakerSecure() {

        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(this, SecureCameraActivity.class);
        String time = ""+new Date().getTime();
        String filename = "/" + Environment.DIRECTORY_DCIM + "/" +  "cs_" + time + ".jpg";
        String thumbnail = "/" + Environment.DIRECTORY_DCIM + "/" +  "cs_" + time + "_thumb.jpg";
        intent.putExtra(SecureCameraActivity.FILENAME, filename ) ;
        intent.putExtra(SecureCameraActivity.THUMBNAIL, thumbnail ) ;

        // start the secure image capture Intent
        startActivityForResult(intent, REQUEST_TAKE_PICTURE_SECURE);
    }

    void startFilePicker() {
        Intent selectFile = new Intent(Intent.ACTION_GET_CONTENT);
        selectFile.setType("file/*");
        Intent intentChooser = Intent.createChooser(selectFile, "Select File");

        if (intentChooser != null)
            startActivityForResult(Intent.createChooser(selectFile, "Select File"), REQUEST_SEND_FILE);
    }

    void startAudioPicker() {


        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        if (!isCallable(intent))
        {
            intent = new Intent("android.provider.MediaStore.RECORD_SOUND");
            intent.addCategory("android.intent.category.DEFAULT");

            if (!isCallable(intent))
            {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");

                if (!isCallable(intent))
                    return;

            }
        }

        startActivityForResult(intent, REQUEST_SEND_AUDIO); // intent and requestCode of 1

    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
            PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void handleSendDelete( final Uri contentUri, final String mimeType, boolean promptDelete ) {
        // if no prompt needed - do not delete original
        if (!promptDelete) {
            handleSend( contentUri, mimeType, false );
            return;
        }
        // if 'delete_unsecured_media' preference is true
        if (SettingActivity.getDeleteUnsecuredMedia(this)) {
            handleSend( contentUri, mimeType, false );
            return;
        }
        // prompt to delete original
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(getString(R.string.delete_original))
        .setMessage(getString(R.string.this_file_will_be_copied))
        .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // send - delete original
                handleSend( contentUri, mimeType, true );
            }
        })
        .setNegativeButton(getString(R.string.keep), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // send - do not delete original
                handleSend( contentUri, mimeType, false );
            }
        })
        .show();
    }

    private void handleSend( Uri contentUri, String mimeType, boolean delete ) {
        try {
            // import
            FileInfo info = SystemServices.getFileInfoFromURI(this, contentUri);
            String sessionId = getCurrentSessionId();
            Uri vfsUri = IocVfs.importContent(sessionId, info.path, mimeType);
            // send
            boolean sent = handleSend(vfsUri, (mimeType==null) ? info.type : mimeType);
            if (!sent) {
                // not deleting if not sent
                return;
            }
            // autu delete
            if (delete) {
                boolean deleted = delete(contentUri);
                if (!deleted) {
                    throw new IOException("Error deleting " + contentUri);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error sending file", Toast.LENGTH_LONG).show(); // TODO i18n
            e.printStackTrace();
        }
    }

    private boolean delete(Uri uri) {
        if (uri.getScheme().equals("content")) {
            int deleted = getContentResolver().delete(uri,null,null);
            return deleted == 1;
        }
        if (uri.getScheme().equals("file")) {
            java.io.File file = new java.io.File(uri.toString().substring(5));
            return file.delete();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SEND_IMAGE || requestCode == REQUEST_SEND_FILE || requestCode == REQUEST_SEND_AUDIO) {
                Uri uri = resultIntent.getData() ;
                if( uri == null ) {
                    return ;
                }
                boolean promptDelete = (requestCode == REQUEST_SEND_AUDIO); // prompt to delete original
                handleSendDelete(uri, null, promptDelete);
            }
            else if (requestCode == REQUEST_TAKE_PICTURE)
            {
                /**
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(mLastPhoto);
                this.sendBroadcast(mediaScanIntent);
                */

                File file = new File(getRealPathFromURI(mLastPhoto));
                final Handler handler = new Handler();
                MediaScannerConnection.scanFile(
                        this, new String[] { file.toString() }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, final Uri uri) {

                                handler.post( new Runnable() {
                                    @Override
                                    public void run() {
                                        handleSendDelete(mLastPhoto, "image/*", true);
                                    }
                                });
                            }
                        });
            }
            else if (requestCode == REQUEST_TAKE_PICTURE_SECURE)
            {
                String filename = resultIntent.getStringExtra(SecureCameraActivity.FILENAME);
                String mimeType = resultIntent.getStringExtra(SecureCameraActivity.MIMETYPE);
                Uri uri = Uri.parse("file:" + filename);
                handleSend(uri,mimeType);
            }
            else if (requestCode == REQUEST_SETTINGS)
            {

                try {
                    mApp.getRemoteImService().updateStateFromSettings();
                } catch (Exception e) {
                    Log.e(ImApp.LOG_TAG,"unable to update service settings",e);
                }

                finish();
                Intent intent = new Intent(getApplicationContext(), NewChatActivity.class);
                startActivity(intent);

            }

            if (requestCode == REQUEST_PICK_CONTACTS) {

                String username = resultIntent.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);
                long providerId = resultIntent.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER,-1);

                String message = resultIntent.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_MESSAGE);
                try {

                    IChatSession chatSession = this.getCurrentChatSession();
                    if (chatSession != null && chatSession.isGroupChatSession()) {
                        chatSession.inviteContact(username);
                        showInvitationHasSent(username);
                    } else {
                        startChat(providerId, username,true, message);
                    }
                } catch (RemoteException e) {
                    mHandler.showServiceErrorAlert("Error picking contacts");
                    Log.d(ImApp.LOG_TAG,"error picking contact",e);
                }
            }

            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
                    resultIntent);

            if (scanResult != null) {
                String xmppUri = scanResult.getContents();
                String result = null;
                if (xmppUri.startsWith("xmpp"))
                    result = XmppUriHelper.getOtrFingerprint(xmppUri);

                if (getCurrentChatView()!=null && result != null)
                    getCurrentChatView().verifyScannedFingerprint(result);
                else
                {
                    //add new contact?
                }


            }
        }
    }

    IChatSession getCurrentChatSession() {
        int currentPos = mChatPager.getCurrentItem();
        if (currentPos == 0)
            return null;
        Cursor cursorChats = mChatPagerAdapter.getCursor();
        cursorChats.moveToPosition(currentPos - 1);
        long providerId = cursorChats.getLong(ChatView.PROVIDER_COLUMN);
        String username = cursorChats.getString(ChatView.USERNAME_COLUMN);
        IChatSessionManager sessionMgr = getChatSessionManager(providerId);
        if (sessionMgr != null) {
            try {
                IChatSession session = sessionMgr.getChatSession(username);

                if (session == null)
                    session = sessionMgr.createChatSession(username, false);

                return session;
            } catch (RemoteException e) {

                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }

        return null;
    }

    private String getCurrentSessionId() throws Exception {
        return ""+getCurrentChatSession().getId();
    }

    private IChatSessionManager getChatSessionManager(long providerId) {
        IImConnection conn = mApp.getConnection(providerId);

        if (conn != null) {
            try {
                return conn.getChatSessionManager();
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }
        return null;
    }


  //----------------------------------------
    /**
     * This method is used to get real path of file from from uri
     *
     * @param contentUri
     * @return String
     */
    //----------------------------------------
    public String getRealPathFromURI(Uri contentUri)
    {
        try
        {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = managedQuery(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch (Exception e)
        {
            return contentUri.getPath();
        }
    }

    private boolean handleSend(Uri uri, String mimeType) {
        try {
            FileInfo info = SystemServices.getFileInfoFromURI(this, uri);

            if (info != null && info.path != null && IocVfs.exists(info.path))
            {
                IChatSession session = getCurrentChatSession();

                if (session != null) {
                    if (info.type == null)
                        if (mimeType != null)
                            info.type = mimeType;
                        else
                            info.type = "application/octet-stream";

                    String offerId = UUID.randomUUID().toString();
                    session.offerData(offerId, info.path, info.type );
                    ChatView cView = getCurrentChatView();
                    int type = cView.isOtrSessionVerified() ? Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED : Imps.MessageType.OUTGOING_ENCRYPTED;
                    Imps.insertMessageInDb(
                            getContentResolver(), false, session.getId(), true, null, uri.toString(),
                            System.currentTimeMillis(), type,
                            0, offerId, info.type);
                    return true; // sent
                }
            }
            else
            {
                Toast.makeText(this, R.string.sorry_we_cannot_share_that_file_type, Toast.LENGTH_LONG).show();
            }
        } catch (RemoteException e) {
           Log.e(ImApp.LOG_TAG,"error sending file",e);
        }
        return false; // was not sent
    }

    void showInvitationHasSent(String contact) {
        Toast.makeText(NewChatActivity.this, getString(R.string.invitation_sent_prompt, contact),
                Toast.LENGTH_SHORT).show();
    }

    /** Show the context menu on a history item. */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        ChatView chatView = getCurrentChatView ();

        if (chatView != null)
        {
            AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            mMessageContextMenuHandler.mPosition = info.position;
            Cursor cursor =  chatView.getMessageAtPosition(info.position);
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(Imps.Messages.TYPE));
            if (type == Imps.MessageType.OUTGOING) {
                android.view.MenuItem mi = menu.add(0, MENU_RESEND, 0, R.string.menu_resend);

                mi.setOnMenuItemClickListener(
                        mMessageContextMenuHandler);



            }


        }
    }

    final class MessageContextMenuHandler implements android.view.MenuItem.OnMenuItemClickListener {
        int mPosition;


        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {

            ChatView chatView = getCurrentChatView ();

            if (chatView != null)
            {
                Cursor c;
                c =  chatView.getMessageAtPosition(mPosition);

                switch (item.getItemId()) {
                case MENU_RESEND:
                    String text = c.getString(c.getColumnIndexOrThrow(Imps.Messages.BODY));
                    chatView.getComposedMessage().setText(text);
                    break;
                default:
                    return false;
                }            return false;
            }
            else
                return false;
        }
    }

    public class ChatViewPagerAdapter extends DynamicPagerAdapter {
        Cursor mCursor;
        boolean mDataValid;

        public ChatViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public Cursor getCursor() {
            return mCursor;
        }

        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor == mCursor) {
                return null;
            }
            Cursor oldCursor = mCursor;
            mCursor = newCursor;
            if (newCursor != null) {
                mDataValid = true;


            } else {
                mDataValid = false;
            }
            notifyDataSetChanged();
            // notify the observers about the new cursor
            refreshChatViews();
            
            return oldCursor;
        }

        @Override
        public int getCount() {
            if (mCursor != null)
                return mCursor.getCount() + 1;
            else
                return 0;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0)
            {
                if (mContactList == null)
                    mContactList = new ContactListFragment();


                return mContactList;
            }
            else
            {
                int positionMod = position - 1;

                mCursor.moveToPosition(positionMod);
                long contactChatId = mCursor.getLong(ChatView.CONTACT_ID_COLUMN);
                String contactName = mCursor.getString(ChatView.USERNAME_COLUMN);
                long providerId = mCursor.getLong(ChatView.PROVIDER_COLUMN);
                
                int chatType = mCursor.getInt(ChatView.TYPE_COLUMN);
                

                return ChatViewFragment.newInstance(contactChatId, contactName, providerId);
            }
        }

        @Override
        public int getItemPosition(Object object) {

            if (object instanceof ChatViewFragment)
            {
                ChatViewFragment cvFrag = (ChatViewFragment)object;
                ChatView view = cvFrag.getChatView();
                long viewChatId = view.mLastChatId;
                int position = PagerAdapter.POSITION_NONE;

                // TODO: cache positions so we don't scan the cursor every time
                if (mCursor != null && mCursor.getCount() > 0)
                {
                    mCursor.moveToFirst();

                    int posIdx = 1;

                    do {
                        long chatId = mCursor.getLong(ChatView.CHAT_ID_COLUMN);

                        if (chatId == viewChatId)
                        {
                            position = posIdx;
                            break;
                        }

                        posIdx++;
                    }
                    while (mCursor.moveToNext());

                }

               //` Log.d(TAG, "position of " + cvFrag.getArguments().getString("contactName") + " = " + position);
                return position;

            }
            else if (object instanceof ContactListFragment)
            {
                return 0;

            }
            else {
                throw new RuntimeException("got asked about an unknown fragment");
            }
        }


        @Override
        public CharSequence getPageTitle(int position) {

            if (position == 0 || mCursor == null)
            {
                if (mShowChatsOnly)
                    return getString(R.string.title_chats);
                else
                    return getString(R.string.contacts);
            }
            else
            {
                int positionMod = position - 1;

                mCursor.moveToPosition(positionMod);
                if (!mCursor.isAfterLast())
                {


                    String nickname = mCursor.getString(ChatView.NICKNAME_COLUMN);
                    int presence = mCursor.getInt(ChatView.PRESENCE_STATUS_COLUMN);
                    int type = mCursor.getInt(ChatView.TYPE_COLUMN);

                    BrandingResources brandingRes = mApp.getBrandingResource(mCursor.getInt(ChatView.PROVIDER_COLUMN));


                    SpannableString s = null;

                    Drawable statusIcon = null;

                    if (Imps.Contacts.TYPE_GROUP == type)
                    {
                        s = new SpannableString(nickname);
                    }
                    else
                    {
                        s = new SpannableString("+ " + nickname);
                        statusIcon = brandingRes.getDrawable(PresenceUtils.getStatusIconId(presence));
                        statusIcon.setBounds(0, 0, statusIcon.getIntrinsicWidth(),
                                statusIcon.getIntrinsicHeight());
                        s.setSpan(new ImageSpan(statusIcon), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

                    }


                    return s;

                }
                else
                    return "";//unknown title
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int pos) {
            Object item = super.instantiateItem(container, pos);
            return item;
        }

        @Override
        public void destroyItem(ViewGroup container, int pos, Object object) {

            super.destroyItem(container, pos, object);
        }

        public ChatView getChatViewAt(int pos) {
            if (pos > 0)
            {
                ChatViewFragment frag = ((ChatViewFragment)getItemAt(pos));

                if (frag != null)
                    return frag.getChatView();
            }

            return null; //this can happen if the user is quickly closing chats; just return null and swallow the event
            //throw new RuntimeException("could not get chat view at " + pos);
        }
    }


    private void initConnections ()
    {
        getSupportLoaderManager().initLoader(CHAT_PAGE_LOADER_ID, null, new LoaderCallbacks<Cursor>() {

            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                CursorLoader loader = new CursorLoader(NewChatActivity.this, Imps.Provider.CONTENT_URI_WITH_ACCOUNT, ContactListFragment.PROVIDER_PROJECTION,
                        Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL",

                        new String[] { ImApp.IMPS_CATEGORY } ,
                        Imps.Provider.DEFAULT_SORT_ORDER);
                loader.setUpdateThrottle(50L);
                return loader;
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {

                if (newCursor != null && newCursor.getCount() > 0)
                {
                    mAccountIds = new Long[newCursor.getCount()][2];
                    newCursor.moveToFirst();
                    int activeAccountIdColumn = 4;
                    int activeProviderIdColumn = 0;

                    for (int i = 0; i < mAccountIds.length; i++)
                    {
                        mAccountIds[i][0] = newCursor.getLong(activeAccountIdColumn);
                        mAccountIds[i][1] = newCursor.getLong(activeProviderIdColumn);

                        newCursor.moveToNext();

                    }

                    for (int i = 0; i < mAccountIds.length; i++)
                        initConnection(mAccountIds[i][0],mAccountIds[i][1]);

                    mLastAccountId = mAccountIds[0][0];
                    mLastProviderId = mAccountIds[0][1];

                    newCursor.moveToFirst();
                    
                    initChats();


                }
                else
                {
                    //no configured accounts, prompt to setup
                    Intent intent = new Intent(NewChatActivity.this, AccountWizardActivity.class);
                    startActivity(intent);
                    finish();
                }


            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mAccountIds = null;
            }
        });

    }


    public void unregisterSubListeners ()
    {
        if (mAccountIds != null)
            for (int i = 0; i < mAccountIds.length; i++)
            {
                IImConnection conn = initConnection(mAccountIds[i][0],mAccountIds[i][1]);
                if (conn != null)
                {
                    try {
                        conn.getContactListManager().unregisterSubscriptionListener(mSubscriptionListener);
                    } catch (RemoteException e1) {
                        Log.e(ImApp.LOG_TAG,"error registering listener",e1);

                    }

                }
            }
    }

    public IImConnection initConnection (long accountId, long providerId)
    {

        IImConnection conn = ((ImApp)getApplication()).getConnection(providerId);

        if (conn == null)
        {
            try {
             conn =  ((ImApp)getApplication()).createConnection(providerId, accountId);
            } catch (RemoteException e) {
               Log.e(ImApp.LOG_TAG,"error creating connection",e);
            }

      
            if (conn != null)
            {
    
                try {
                    conn.getContactListManager().registerSubscriptionListener(mSubscriptionListener);
                   // conn.getContactListManager().registerContactListListener(mContactListListener);
                } catch (RemoteException e1) {
                    Log.e(ImApp.LOG_TAG,"error registering listener",e1);
    
                }
            }


        }

        return conn;

    }

    public void updateChatList ()
    {

        if (mContactList != null && mContactList.mFilterView != null)
        {
            mLastPagePosition = -1;
              Uri baseUri = Imps.Contacts.CONTENT_URI_CHAT_CONTACTS_BY;
              Uri.Builder builder = baseUri.buildUpon();

              mContactList.mFilterView.doFilter(builder.build(), null);
        }
        
        mChatPager.invalidate();
    }



    public static class ContactListFragment extends Fragment implements ContactListListener
    {


        private static final String[] PROVIDER_PROJECTION = {
                                                             Imps.Provider._ID,
                                                             Imps.Provider.NAME,
                                                             Imps.Provider.FULLNAME,
                                                             Imps.Provider.CATEGORY,
                                                             Imps.Provider.ACTIVE_ACCOUNT_ID,
                                                             Imps.Provider.ACTIVE_ACCOUNT_USERNAME,
                                                             Imps.Provider.ACTIVE_ACCOUNT_PW,
                                                             Imps.Provider.ACTIVE_ACCOUNT_LOCKED,
                                                             Imps.Provider.ACTIVE_ACCOUNT_KEEP_SIGNED_IN,
                                                             Imps.Provider.ACCOUNT_PRESENCE_STATUS,
                                                             Imps.Provider.ACCOUNT_CONNECTION_STATUS, };



        static final int PROVIDER_ID_COLUMN = 0;
        static final int PROVIDER_NAME_COLUMN = 1;
        static final int PROVIDER_FULLNAME_COLUMN = 2;
        static final int PROVIDER_CATEGORY_COLUMN = 3;
        static final int ACTIVE_ACCOUNT_ID_COLUMN = 4;
        static final int ACTIVE_ACCOUNT_USERNAME_COLUMN = 5;
        static final int ACTIVE_ACCOUNT_PW_COLUMN = 6;
        static final int ACTIVE_ACCOUNT_LOCKED = 7;
        static final int ACTIVE_ACCOUNT_KEEP_SIGNED_IN = 8;
        static final int ACCOUNT_PRESENCE_STATUS = 9;
        static final int ACCOUNT_CONNECTION_STATUS = 10;

        ContactListFilterView mFilterView = null;

        ImApp mApp = null;

        private Handler mPresenceHandler = new Handler()
        {

            @Override
            public void handleMessage(Message msg) {


           //     mPresenceView.refreshLogginInStatus();

                super.handleMessage(msg);
            }
        };


        /**
          * The Fragment's UI is just a simple text view showing its
          * instance number.
          */
         @Override
         public View onCreateView(LayoutInflater inflater, ViewGroup container,
                 Bundle savedInstanceState) {

              mFilterView = (ContactListFilterView) inflater.inflate(
                     R.layout.contact_list_filter_view, null);


             mFilterView.setListener(this);
             mFilterView.setLoaderManager(getLoaderManager(), CONTACT_LIST_LOADER_ID);

             TextView txtEmpty = (TextView)mFilterView.findViewById(R.id.empty);

             txtEmpty.setOnClickListener(new OnClickListener ()
             {

                @Override
                public void onClick(View v) {

                        ((NewChatActivity)getActivity()).startContactPicker();
                }

             });

             ((AbsListView)mFilterView.findViewById(R.id.filteredList)).setEmptyView(txtEmpty);

             Uri baseUri = Imps.Contacts.CONTENT_URI_CHAT_CONTACTS_BY;
             Uri.Builder builder = baseUri.buildUpon();
             mFilterView.doFilter(builder.build(), null);

              return mFilterView;

         }

         @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            mApp = ((ImApp)activity.getApplication());
            mApp.registerForConnEvents(mPresenceHandler);


        }

        @Override
        public void onDetach() {
            super.onDetach();

            mApp.unregisterForConnEvents(mPresenceHandler);
            mApp = null;

        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
        }



        @Override
        public void openChat(Cursor c) {

            NewChatActivity activity = (NewChatActivity)getActivity();
            activity.openExistingChat(c);

        }

        @Override
        public void showProfile (Cursor c)
        {
            if (c != null) {
                long chatContactId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));

                long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));
                long accountId = c.getLong(c.getColumnIndex(Imps.Contacts.ACCOUNT));

                Uri data = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, chatContactId);

                Intent intent = new Intent(Intent.ACTION_VIEW, data);
                intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, providerId);
                intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, accountId);

                startActivity(intent);

            }
        }


    }




    private void openExistingChat(Cursor c) {

        if (c != null && (!  c.isAfterLast())) {
            String username = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));

            startChat(providerId,username, false, null);
        }
        else
            updateChatList();
    }

    private void startChat (long providerId, String username, boolean isNewChat, String message)
    {
        IImConnection conn = mApp.getConnection(providerId);

        if (conn != null)
        {
            try {
                IChatSessionManager manager = conn.getChatSessionManager();
                IChatSession session = manager.getChatSession(username);
                if (session == null && manager != null) {
                    // Create session.  Stash requested contact ID for when we get called back.
                    session = manager.createChatSession(username, isNewChat);
                    if (session != null)
                        mRequestedChatId = session.getId();

                    if (!showChat(session.getId())) {
                        // We have a session, but it's not in the cursor yet
                        mRequestedChatId = session.getId();
                        session.reInit();
                    }
                    
                    if (message != null)
                        session.sendMessage(message);

                } else {
                    // Already have session
                    if (!showChat(session.getId())) {
                        // We have a session, but it's not in the cursor yet
                        mRequestedChatId = session.getId();
                        session.reInit();
                    }

                }

                updateChatList();
            } catch (RemoteException e) {
              //  mHandler.showServiceErrorAlert(e.getMessage());
                LogCleaner.debug(ImApp.LOG_TAG, "remote exception starting chat");

            }

        }
        else
        {
            LogCleaner.debug(ImApp.LOG_TAG, "could not start chat as connection was null");
        }
    }

    public static class ChatViewFragment extends Fragment {

         ChatView mChatView;

        /**
         * Create a new instance of CountingFragment, providing "num"
         * as an argument.
         * @param providerId
         * @param contactName
         */
        static ChatViewFragment newInstance(long chatContactId, String contactName, long providerId) {

            ChatViewFragment f = new ChatViewFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putLong("contactChatId", chatContactId);
            args.putString("contactName", contactName);
            args.putLong("providerId", providerId);
            f.setArguments(args);

//            Log.d(TAG, "CVF new " + contactName);
            return f;
        }

        public ChatViewFragment() {
//            Log.d(TAG, "CVF construct " + super.toString());
        }

        @Override
        public String toString() {
            return super.toString() + " -> " + getArguments().getString("contactName");
        }

        public void onSelected(ImApp app) {

            //app.dismissChatNotification(getArguments().getLong("providerId"), getArguments().getString("contactName"));

            if (mChatView != null)
                mChatView.setSelected(true);


        }

        public void onDeselected(ImApp app) {
            if (mChatView != null)
                mChatView.setSelected(false);
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

//            Log.d(TAG, "CVF create " + getArguments().getString("contactName"));
        }

        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            long chatContactId = getArguments().getLong("contactChatId");
            mChatView = (ChatView)inflater.inflate(R.layout.chat_view, container, false);
            mChatView.bindChat(chatContactId);

            return mChatView;
        }

        public void onServiceConnected() {
            if (isResumed()) {
                mChatView.onServiceConnected();
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            mChatView.startListening();
        }

        @Override
        public void onPause() {
            super.onPause();

            mChatView.stopListening();
        }

        @Override
        public void onDestroy() {
            mChatView.unbind();
            super.onDestroy();
        }

        public ChatView getChatView() {
            return mChatView;
        }
    }

    public ChatView getCurrentChatView ()
    {
        int cItemIdx;

        // FIXME why is mChatPagerAdapter null here?  Is this called after onDestroy?
        if (mChatPagerAdapter != null && (cItemIdx = mChatPager.getCurrentItem()) > 0)
        {
            return mChatPagerAdapter.getChatViewAt(cItemIdx);
        }
        else
            return null;
    }




    private void showGroupChatDialog ()
    {

     // This example shows how to add a custom layout to an AlertDialog
        LayoutInflater factory = LayoutInflater.from(this);

        final View dialogGroup = factory.inflate(R.layout.alert_dialog_group_chat, null);
        TextView tvServer = (TextView) dialogGroup.findViewById(R.id.chat_server);
       // tvServer.setText(ImApp.DEFAULT_GROUPCHAT_SERVER);// need to make this a list

        final Spinner listAccounts = (Spinner) dialogGroup.findViewById(R.id.choose_list);
        setupAccountSpinner(listAccounts);
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.create_or_join_group_chat)
            .setView(dialogGroup)
            .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked OK so do some stuff */

                    String chatRoom = null;
                    String chatServer = null;
                    String nickname = null;

                    TextView tv = (TextView)dialogGroup.findViewById(R.id.chat_room);
                    chatRoom = tv.getText().toString();

                    tv = (TextView) dialogGroup.findViewById(R.id.chat_server);
                    chatServer = tv.getText().toString();

                    tv = (TextView) dialogGroup.findViewById(R.id.nickname);
                    nickname = tv.getText().toString();

                    try
                    {
                        IImConnection conn = mApp.getConnection(mLastProviderId);
                        if (conn.getState() == ImConnection.LOGGED_IN)
                            startGroupChat (chatRoom, chatServer, nickname, conn);
                        else
                        {
                            //can't start group chat
                            mHandler.showAlert("Group Chat","Please enable your account to join a group chat");
                        }
                    } catch (RemoteException re) {

                    }

                    dialog.dismiss();

                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                    dialog.dismiss();
                }
            })
            .create().show();



    }

    private void setupAccountSpinner (Spinner spinner)
    {
        final Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;

        final Cursor cursorProviders = managedQuery(uri,  PROVIDER_PROJECTION,
        Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL" /* selection */,
        new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
        Imps.Provider.DEFAULT_SORT_ORDER);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, cursorProviders, new String[] { Imps.Provider.ACTIVE_ACCOUNT_USERNAME},
                new int[] { android.R.id.text1 });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (cursorProviders.getCount() > 0)
        {
            cursorProviders.moveToFirst();
            mLastProviderId = cursorProviders.getLong(PROVIDER_ID_COLUMN);
            mLastAccountId = cursorProviders.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                        int arg2, long arg3) {
                    cursorProviders.moveToPosition(arg2);

                    mLastProviderId = cursorProviders.getLong(PROVIDER_ID_COLUMN);
                    mLastAccountId = cursorProviders.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
                 }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // TODO Auto-generated method stub

                }
            });
        }
        else
        {
            spinner.setVisibility(View.GONE);
        }

    }



    private IImConnection mLastConnGroup = null;

    public void startGroupChat (String room, String server, String nickname, IImConnection conn)
    {
        mLastConnGroup = conn;

        new AsyncTask<String, Long, String>() {

            private ProgressDialog dialog;


            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(NewChatActivity.this);

                dialog.setMessage(getString(R.string.connecting_to_group_chat_));
                dialog.setCancelable(true);
                dialog.show();
            }

            @Override
            protected String doInBackground(String... params) {

                String roomAddress = (params[0] + '@' + params[1]).toLowerCase(Locale.US).replace(' ', '_');
                String nickname = params[2];

                try {
                    IChatSessionManager manager = mLastConnGroup.getChatSessionManager();
                    IChatSession session = manager.getChatSession(roomAddress);
                    if (session == null) {
                        session = manager.createMultiUserChatSession(roomAddress, nickname);
                        if (session != null)
                        {
                            mRequestedChatId = session.getId();
                            publishProgress(mRequestedChatId);

                        } else {
                            return getString(R.string.unable_to_create_or_join_group_chat);

                        }
                    } else {
                        mRequestedChatId = session.getId();
                        publishProgress(mRequestedChatId);
                    }

                    return null;

                } catch (RemoteException e) {
                    return e.toString();
                }

              }

			@Override
            protected void onProgressUpdate(Long... showChatId) {
                showChat(showChatId[0]);
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                if (dialog.isShowing()) {
                    dialog.dismiss();
                }

                if (result != null)
                {
                    mHandler.showServiceErrorAlert(result);

                }


            }
        }.execute(room, server, nickname);



    }

    void acceptInvitation(long providerId, long invitationId) {
        try {

            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.acceptInvitation(invitationId);
            }
        } catch (RemoteException e) {

            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "accept invite error",e);
        }
    }

    void declineInvitation(long providerId, long invitationId) {
        try {
            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.rejectInvitation(invitationId);
            }
        } catch (RemoteException e) {

            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "decline invite error",e);
        }
    }

    void showSubscriptionDialog (final long subProviderId, final String subFrom)
    {
        if (! ((Activity) this).isFinishing()) {

            mHandler.postDelayed(new Runnable()
            {

                @Override
                public void run ()
                {
                    new AlertDialog.Builder(NewChatActivity.this)
                    .setTitle(getString(R.string.subscriptions))
                    .setMessage(getString(R.string.subscription_prompt,subFrom))
                    .setPositiveButton(R.string.approve_subscription, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {

                            approveSubscription(subProviderId, subFrom);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.decline_subscription, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {

                            declineSubscription(subProviderId, subFrom);
                            dialog.dismiss();
                        }
                    })
                    .create().show();
                }
            },500);
        }
    }

    void approveSubscription(long providerId, String userName) {
        IImConnection conn = mApp.getConnection(providerId);

        if (conn != null)
        {
            try {
                IContactListManager manager = conn.getContactListManager();

                manager.approveSubscription(new Contact(new XmppAddress(userName),userName));
            } catch (RemoteException e) {

                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "approve sub error",e);
            }
        }
    }

    void declineSubscription(long providerId, String userName) {
        IImConnection conn = mApp.getConnection(providerId);

        if (conn != null)
        {
            try {
                IContactListManager manager = conn.getContactListManager();
                manager.declineSubscription(new Contact(new XmppAddress(userName),userName));
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "decline sub error",e);
            }
        }
    }


    long getLastAccountId() {
        return mLastAccountId;
    }

    long getLastProviderId() {
        return mLastProviderId;
    }

    void setLastProviderId(long mLastProviderId) {
        this.mLastProviderId = mLastProviderId;
    }

    public class ProviderListItemFactory implements LayoutInflater.Factory {
        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            if (name != null && name.equals(ProviderListItem.class.getName())) {
                return new ProviderListItem(context, NewChatActivity.this, null);
            }
            return null;
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void doHardShutdown() {

        for (IImConnection conn : mApp.getActiveConnections())
        {
               try {
                conn.logout();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        finish();
        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        // Request lock
        intent.putExtra("doLock", true);
        // Clear the backstack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 11)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

   }


    private final ISubscriptionListener.Stub mSubscriptionListener = new ISubscriptionListener.Stub() {

        @Override
        public void onSubScriptionRequest(Contact from, long providerId, long accountId) {
           
            showSubscriptionDialog (providerId, from.getAddress().getAddress());
            
        }

        @Override
        public void onSubscriptionApproved(Contact contact, long providerId, long accountId) {

        }

        @Override
        public void onSubscriptionDeclined(Contact contact, long providerId, long accountId) {

        }

    };
    
    private final IContactListListener.Stub mContactListListener = new IContactListListener.Stub ()
    {
        
        @Override
        public IBinder asBinder() {
            
            return null;
        }

        @Override
        public void onContactChange(int type, IContactList list, Contact contact)
                throws RemoteException {
           
            
        }

        @Override
        public void onAllContactListsLoaded() throws RemoteException {
          
            Log.d(ImApp.LOG_TAG, "onAllContactListsLoaded");
        }

        @Override
        public void onContactsPresenceUpdate(Contact[] contacts) throws RemoteException {
          
            
        }

        @Override
        public void onContactError(int errorType, ImErrorInfo error, String listName,
                Contact contact) throws RemoteException {
          
            
        }
        
    };

    private synchronized Imps.ProviderSettings.QueryMap getGlobalSettings() {
        if (mGlobalSettings == null) {

            ContentResolver contentResolver = getContentResolver();

            Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS)},null);

            if (cursor == null)
                return null;

            mGlobalSettings = new Imps.ProviderSettings.QueryMap(cursor, contentResolver, Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS, true, mHandler);
        }

        return mGlobalSettings;
    }

    public int getOtrPolicy() {
        int otrPolicy = OtrPolicy.OPPORTUNISTIC;

        String otrModeSelect = getGlobalSettings().getOtrMode();

        if (otrModeSelect.equals("auto")) {
            otrPolicy = OtrPolicy.OPPORTUNISTIC;
        } else if (otrModeSelect.equals("disabled")) {
            otrPolicy = OtrPolicy.NEVER;

        } else if (otrModeSelect.equals("force")) {
            otrPolicy = OtrPolicy.OTRL_POLICY_ALWAYS;

        } else if (otrModeSelect.equals("requested")) {
            otrPolicy = OtrPolicy.OTRL_POLICY_MANUAL;
        }
        return otrPolicy;
    }

    private static final String[] PROVIDER_PROJECTION = {
                                                         Imps.Provider._ID,
                                                         Imps.Provider.NAME,
                                                         Imps.Provider.FULLNAME,
                                                         Imps.Provider.CATEGORY,
                                                         Imps.Provider.ACTIVE_ACCOUNT_ID,
                                                         Imps.Provider.ACTIVE_ACCOUNT_USERNAME,
                                                         Imps.Provider.ACTIVE_ACCOUNT_PW,
                                                         Imps.Provider.ACTIVE_ACCOUNT_LOCKED,
                                                         Imps.Provider.ACTIVE_ACCOUNT_KEEP_SIGNED_IN,
                                                         Imps.Provider.ACCOUNT_PRESENCE_STATUS,
                                                         Imps.Provider.ACCOUNT_CONNECTION_STATUS
                                                        };

    static final int PROVIDER_ID_COLUMN = 0;
    static final int PROVIDER_NAME_COLUMN = 1;
    static final int PROVIDER_FULLNAME_COLUMN = 2;
    static final int PROVIDER_CATEGORY_COLUMN = 3;
    static final int ACTIVE_ACCOUNT_ID_COLUMN = 4;
    static final int ACTIVE_ACCOUNT_USERNAME_COLUMN = 5;
    static final int ACTIVE_ACCOUNT_PW_COLUMN = 6;
    static final int ACTIVE_ACCOUNT_LOCKED = 7;
    static final int ACTIVE_ACCOUNT_KEEP_SIGNED_IN = 8;
    static final int ACCOUNT_PRESENCE_STATUS = 9;
    static final int ACCOUNT_CONNECTION_STATUS = 10;

}

/* Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.otr.app.im.app.test;

import android.content.ContentUris;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.AccountActivity;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.provider.Imps;

/**
 * @author devrandom
 */
public class AccountActivityTest extends ActivityUnitTestCase<AccountActivity> {
  private AccountActivity mActivity;
  private View mView;
  private EditText mEditUserAccount;
  private EditText mEditPass;
  private Button mBtnSignin;

  public AccountActivityTest() {
    super(AccountActivity.class);
  }
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      Intent intent = new Intent(getInstrumentation().getTargetContext(), AccountActivity.class);
      intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, 1));
      startActivity(intent, null, null);
      mActivity = this.getActivity();
      mView = mActivity.findViewById(R.id.acct_act_scrollview);
      assertNotNull(mView);
      mEditUserAccount = (EditText)mActivity.findViewById(R.id.edtName);
      mEditPass = (EditText)mActivity.findViewById(R.id.edtPass);
      mBtnSignin = (Button)mActivity.findViewById(R.id.btnSignIn);
  }
  
  public void testPreconditions() {
    // Check that the title was set correctly
    ImApp app = ImApp.getApplication(mActivity);
    assertEquals(mActivity.getString(R.string.add_account, app.getProvider(1).mFullName),
        mActivity.getTitle());
  }
  
  public void testSignIn() {
    // Enter values, checking that the sigin button was enabled at the right time
    assertFalse(mBtnSignin.isEnabled());
    mEditUserAccount.setText("user@localhost.localdomain");
    assertFalse(mBtnSignin.isEnabled());
    mEditPass.setText("pass");
    assertTrue(mBtnSignin.isEnabled());
    mActivity.findViewById(R.id.btnSignIn).performClick();
    
    // Check that the signin activity was started
    Intent started = getStartedActivityIntent();
    assertEquals(".app.SigningInActivity", started.getComponent().getShortClassName());
    assertEquals("pass", started.getStringExtra(ImApp.EXTRA_INTENT_PASSWORD));
    assertEquals(ContentUris.withAppendedId(Imps.Account.CONTENT_URI, 1), started.getData());
  }
}

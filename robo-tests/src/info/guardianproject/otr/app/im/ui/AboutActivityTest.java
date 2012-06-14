/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package info.guardianproject.otr.app.im.ui;

import static junit.framework.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.RobolectricTestRunner;

import android.view.View;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.ui.AboutActivity;

/**
 * @author devrandom
 */
@RunWith(RobolectricTestRunner.class)
public class AboutActivityTest {
    private AboutActivity mActivity;
    private View mView;

    @Before
    public void setUp() throws Exception {
        mActivity = new AboutActivity();
        mActivity.onCreate(null);
        mActivity.onStart();
        mView = mActivity.findViewById(R.id.WizardTextBody);
    }

    @Test
    public void testPreconditions() {
        assertNotNull(mView);
    }
}

/*
 * Copyright (c) 2015. Catch Inc,
 */

package catchla.yep.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.bluelinelabs.logansquare.LoganSquare;

import java.io.IOException;

import catchla.yep.Constants;
import catchla.yep.R;
import catchla.yep.adapter.TabsAdapter;
import catchla.yep.fragment.UserSuggestionsFragment;
import catchla.yep.model.AccessToken;
import catchla.yep.model.User;
import catchla.yep.util.ThemeUtils;
import catchla.yep.util.Utils;
import catchla.yep.view.TabPagerIndicator;
import catchla.yep.view.TintedStatusFrameLayout;

public class WelcomeActivity extends AccountAuthenticatorActivity implements Constants, View.OnClickListener {

    private static final int REQUEST_ADD_ACCOUNT = 101;

    private ViewPager mViewPager;
    private TabsAdapter mAdapter;
    private TabPagerIndicator mPagerIndicator;
    private TintedStatusFrameLayout mMainContent;
    private Button mSignInButton;
    private Button mSignUpButton;

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mMainContent = (TintedStatusFrameLayout) findViewById(R.id.main_content);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mSignInButton = (Button) findViewById(R.id.sign_in);
        mSignUpButton = (Button) findViewById(R.id.sign_up);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_ACCOUNT: {
                if (resultCode != RESULT_OK) return;
                final AccessToken token;
                try {
                    token = LoganSquare.parse(data.getStringExtra(EXTRA_TOKEN), AccessToken.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                final User user = token.getUser();
                final Account account = new Account(user.getMobile(), ACCOUNT_TYPE);
                final Bundle userData = new Bundle();
                Utils.writeUserToUserData(user, userData);
                final AccountManager am = AccountManager.get(this);
                am.addAccountExplicitly(account, null, userData);
                am.setAuthToken(account, AUTH_TOKEN_TYPE, token.getAccessToken());
                if (Utils.getCurrentAccount(this) == null) {
                    Utils.setCurrentAccount(this, account);
                }
                final Bundle result = new Bundle();
                result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
                setAccountAuthenticatorResult(result);
                if (!getIntent().hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)) {
                    final Intent launcherIntent = new Intent(this, MainActivity.class);
                    startActivity(launcherIntent);
                }
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.layout_welcome_tabs);
        final int primaryColor = ThemeUtils.getColorFromAttribute(this, R.attr.colorPrimary, 0);
        actionBar.setBackgroundDrawable(ThemeUtils.getActionBarBackground(primaryColor, true));
        mPagerIndicator = (TabPagerIndicator) actionBar.getCustomView().findViewById(R.id.pager_indicator);
        setContentView(R.layout.activity_welcome);
        final Toolbar toolbar = (Toolbar) getWindow().findViewById(android.support.v7.appcompat.R.id.action_bar);
        toolbar.setContentInsetsRelative(0, 0);

        mSignInButton.setOnClickListener(this);
        mSignUpButton.setOnClickListener(this);

        mAdapter = new TabsAdapter(actionBar.getThemedContext(), getSupportFragmentManager());
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mMainContent.setDrawColor(true);
        mMainContent.setDrawShadow(false);
        mMainContent.setColor(primaryColor);
        mAdapter.addTab(UserSuggestionsFragment.class, getString(R.string.suggestions), 0, null);
        mAdapter.addTab(UserRankFragment.class, getString(R.string.rank), 0, null);
        mPagerIndicator.setViewPager(mViewPager);
        mPagerIndicator.updateAppearance();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in: {
                startActivityForResult(new Intent(this, SignInActivity.class), REQUEST_ADD_ACCOUNT);
                break;
            }
            case R.id.sign_up: {
                startActivityForResult(new Intent(this, SignUpActivity.class), REQUEST_ADD_ACCOUNT);
                break;
            }
        }
    }

    class TabSpec {
        Class<? extends Fragment> cls;
        CharSequence title;
        int icon;

        TabSpec(Class<? extends Fragment> cls, CharSequence title, int icon, Bundle args) {
            this.cls = cls;
            this.title = title;
            this.icon = icon;
            this.args = args;
        }

        Bundle args;
    }
}
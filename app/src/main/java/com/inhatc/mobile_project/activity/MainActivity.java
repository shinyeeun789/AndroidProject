package com.inhatc.mobile_project.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.inhatc.mobile_project.R;
import com.inhatc.mobile_project.db.MemberInfo;
import com.inhatc.mobile_project.fragment.FragmentHome;
import com.inhatc.mobile_project.fragment.FragmentRanking;
import com.inhatc.mobile_project.fragment.FragmentUser;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    final String TAG="MainActivity";

    private FragmentHome fragmentHome = new FragmentHome();
    private FragmentRanking fragmentRanking = new FragmentRanking();
    private FragmentUser fragmentUser = new FragmentUser();
    private MemberInfo userInfo = new MemberInfo();
    private Bundle bundle = new Bundle();
    


    @Override
    public void onBackPressed() {
        //메인 화면에서 뒤로가기 버튼 클릭 시 바로 종료
        super.onBackPressed();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // 현재 로그인 사용자 인스턴스 가져오기
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null){
            //로그인이 안되있으면 로그인 화면으로
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            // 뒤로가기 버튼 누르면 로그인 화면이나 회원가입 화면으로 이동
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);

        }else {
            //로그인이 되어있으면 loddingActivity로 이동하여 사용자 정보 받아옴
            Intent intent = new Intent(this, LoddingActivity.class);
            startActivityForResult(intent, 1);
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            //사용자 정보를 받아옴
            bundle = data.getBundleExtra("userInfoData");
            initView();
        }
    }

    private void initView() {
        // 프래그먼트 트랜잭션(프래그먼트 백 스택 관리, 프래그먼트 전환 애니메이션 설정 등) 시작

        // add(): 추가, remove(): 삭제, replace(): 전환
        // commit()은 activity가 state 저장하기 전에 이루어져야 함. 저장된 후 불리면 exception 발생
        // commitAllowingStateLoss(): 프래그먼트가 state 저장과 관련없게 작동되도록 사용
        replaceFragment(fragmentHome);
        BottomNavigationView bottomNavigationView = findViewById(R.id.navigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new ItemSelectedListener());   // 메뉴 클릭에 대한 이벤트 처리
        bottomNavigationView.setSelectedItemId(R.id.page_home);     // 선택된 아이템 지정
    }

    class ItemSelectedListener implements BottomNavigationView.OnNavigationItemSelectedListener {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch(menuItem.getItemId()) {
                case R.id.page_home:
                    replaceFragment(fragmentHome);
                    break;
                case R.id.page_ranking:
                    replaceFragment(fragmentRanking);
                    break;
                case R.id.page_user:
                    replaceFragment(fragmentUser);
                    break;
            }
            return true;
        }
    }

    public void replaceFragment(Fragment fragment) {
        //사용자 정보가 들어있는 번들을 fragment에 같이 보냄
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment).commitAllowingStateLoss();
    }

}
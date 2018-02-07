package com.cangwang.bottom;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.cangwang.annotation.ModuleUnit;
import com.cangwang.base.api.ChatApi;
import com.cangwang.core.ModuleApiManager;
import com.cangwang.core.cwmodule.CWModuleContext;
import com.cangwang.core.cwmodule.ex.CWBasicExModule;
import com.cangwang.enums.LayoutLevel;

/**输入模块
 * Created by cangwang on 2018/2/5.
 */

@ModuleUnit(templet = "top",layoutlevel = LayoutLevel.LOW)
public class InputModule extends CWBasicExModule{

    private View inputLayout;
    private EditText inputText;
    private ImageView inputBtn;

    @Override
    public boolean init(CWModuleContext moduleContext, Bundle extend) {
        super.init(moduleContext, extend);
        initView();
        return true;
    }


    private void initView() {
        inputLayout = LayoutInflater.from(context).inflate(R.layout.bottom_input_layout, parentPlugin, true);
        inputText = (EditText) inputLayout.findViewById(R.id.bottom_input_txt);
        inputBtn = (ImageView) inputLayout.findViewById(R.id.bottom_input_btn);
        inputBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString();
                if (!text.isEmpty()){
                    boolean result = ModuleApiManager.getInstance().getApi(ChatApi.class).addChatMsg("cangwang",text);
                    if (result){
                        Toast.makeText(context,"发送成功",Toast.LENGTH_SHORT).show();
                        inputText.setText("");
                    }
                }
            }
        });
    }
}
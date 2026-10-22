package com.ethk3.novaonepro;
import android.app.*;import android.content.DialogInterface;import android.os.Build;import android.text.*;import android.view.WindowManager;import android.view.inputmethod.EditorInfo;import android.widget.*;import java.lang.ref.WeakReference;import java.util.Arrays;
/** Foreground private credential entry; keystore/file work is owned by KeyProvider's worker. */
public final class ApiCredentialSettingsCP12 {
 private ApiCredentialSettingsCP12(){}
 public static void show(Activity activity,Runnable afterSaved){
  EditText input=new EditText(activity);input.setSingleLine(true);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128)});input.setHint("数据接口 API Key");
  if(Build.VERSION.SDK_INT>=26)input.setImeOptions(input.getImeOptions()|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
  String addresses="主接口："+BinanceGameContract.primary().endpoint()+"\n备用："+BinanceGameContract.backup().endpoint()+"\n数据代码："+BinanceGameContract.GAME_CODE+"\n\n";
  String status=KeyProvider.isKeyReady()?"接口地址和访问凭据已就绪。此处仅用于替换你自己的密钥。":"接口地址已内置，当前安装包未配置有效访问凭据。请输入已授权的 API Key。";
  AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(KeyProvider.isKeyReady()?"修改数据接口密钥（可选）":"配置数据接口密钥").setMessage(addresses+status+"保存后加密保存在本设备，覆盖更新继续读取。8—128 位字母或数字。").setView(input).setNegativeButton("取消",(d,w)->input.getText().clear()).setPositiveButton("保存",null).create();
  WeakReference<Activity> owner=new WeakReference<>(activity);WeakReference<AlertDialog> window=new WeakReference<>(dialog);WeakReference<EditText> field=new WeakReference<>(input);android.content.Context app=activity.getApplicationContext();
  dialog.setOnShowListener(ignored->{dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view->{
   char[] chars=new char[input.length()];input.getText().getChars(0,chars.length,chars,0);input.getText().clear();dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
   KeyProvider.configureAsync(app,chars,(saved,reason)->{Activity current=owner.get();if(current==null||current.isFinishing()||current.isDestroyed())return;
    current.runOnUiThread(()->{AlertDialog shown=window.get();EditText entry=field.get();if(saved){if(shown!=null)shown.dismiss();Toast.makeText(current,"凭据已加密保存",Toast.LENGTH_SHORT).show();afterSaved.run();}else if(shown!=null&&shown.isShowing()){shown.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);if(entry!=null)entry.setError(reason.equals("KEY_INPUT_INVALID")?"请输入 8—128 位字母或数字":"保存未完成："+reason+"，请重试配置");}});
   });Arrays.fill(chars,'\0');
  });});dialog.show();
 }
}

package bdkj.com.englishstu.view.Fragment;


import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.EvaluatorListener;
import com.iflytek.cloud.EvaluatorResult;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvaluator;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.orhanobut.logger.Logger;
import com.youdao.sdk.app.Language;
import com.youdao.sdk.app.LanguageUtils;
import com.youdao.sdk.ydonlinetranslate.TranslateErrorCode;
import com.youdao.sdk.ydonlinetranslate.TranslateListener;
import com.youdao.sdk.ydonlinetranslate.TranslateParameters;
import com.youdao.sdk.ydonlinetranslate.Translator;
import com.youdao.sdk.ydtranslate.Translate;

import bdkj.com.englishstu.R;
import bdkj.com.englishstu.base.JsonEntity;
import bdkj.com.englishstu.base.baseView.BaseFragment;
import bdkj.com.englishstu.common.beans.Exam;
import bdkj.com.englishstu.common.beans.Test;
import bdkj.com.englishstu.common.dbinfo.StuDbUtils;
import bdkj.com.englishstu.common.tool.StringUtil;
import bdkj.com.englishstu.common.tool.ToastUtil;
import bdkj.com.englishstu.view.AnswerExamActivity;
import butterknife.BindView;
import butterknife.OnClick;

/**
 * 单词朗读页面
 * A simple {@link Fragment} subclass.
 */
public class TestWordFragment extends BaseFragment {


    @BindView(R.id.tv_word)
    TextView tvWord;
    @BindView(R.id.tv_speck_en)
    TextView tvSpeckEn;
    @BindView(R.id.tv_speck_us)
    TextView tvSpeckUs;
    @BindView(R.id.tv_translate_result)
    TextView tvTranslateResult;
    @BindView(R.id.tv_translate)
    TextView tvTranslate;
    @BindView(R.id.tv_word2)
    TextView tvWord2;
    @BindView(R.id.tv_translate2)
    TextView tvTranslate2;
    @BindView(R.id.tv_speck_en2)
    TextView tvSpeckEn2;
    @BindView(R.id.tv_speck_us2)
    TextView tvSpeckUs2;
    @BindView(R.id.tv_translate_result2)
    TextView tvTranslateResult2;
    @BindView(R.id.tv_word3)
    TextView tvWord3;
    @BindView(R.id.tv_translate3)
    TextView tvTranslate3;
    @BindView(R.id.tv_speck_en3)
    TextView tvSpeckEn3;
    @BindView(R.id.tv_speck_us3)
    TextView tvSpeckUs3;
    @BindView(R.id.tv_translate_result3)
    TextView tvTranslateResult3;
    @BindView(R.id.fl_speck_voice)
    FrameLayout flSpeckVoice;

    // 语音合成对象 科大讯飞合成
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "catherine";
    // 缓冲进度
    private int mPercentForBuffering = 0;
    // 播放进度
    private int mPercentForPlaying = 0;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private Toast progressToast;


    // 评测语种 科大讯飞评测
    private String language;
    // 评测题型
    private String category;
    // 结果等级
    private String result_level;

    private String mLastResult;
    private SpeechEvaluator mIse;


    private Test currentTest;
    private Exam currentExam;
    private Handler handler = new Handler() {
        public void handleMessage(Message message) {
            Translate translate = (Translate) message.getData().getSerializable("result");
            switch (message.what) {
                case 0:
                    tvWord.setText(translate.getQuery());
                    tvTranslate.setText(StringUtil.listStr(translate.getTranslations()));
                    tvSpeckEn.setText("英式发音：["
                            + translate.getUkPhonetic() + "]");
                    tvSpeckUs.setText("美式发音：["
                            + translate.getUsPhonetic() + "]");
                    tvTranslateResult.setText(StringUtil.listStr(translate.getExplains()));
                    break;
                case 1:

                    tvWord2.setText(translate.getQuery());
                    tvTranslate2.setText(StringUtil.listStr(translate.getTranslations()));
                    tvSpeckEn2.setText("英式发音：["
                            + translate.getUkPhonetic() + "]");
                    tvSpeckUs2.setText("美式发音：["
                            + translate.getUsPhonetic() + "]");
                    tvTranslateResult2.setText(StringUtil.listStr(translate.getExplains()));
                    break;
                case 2:
                    tvWord3.setText(translate.getQuery());
                    tvTranslate3.setText(StringUtil.listStr(translate.getTranslations()));
                    tvSpeckEn3.setText("英式发音：["
                            + translate.getUkPhonetic() + "]");
                    tvSpeckUs3.setText("美式发音：["
                            + translate.getUsPhonetic() + "]");
                    tvTranslateResult3.setText(StringUtil.listStr(translate.getExplains()));
                    break;
            }
        }
    };

    @Override
    public int getViewLayout() {
        return R.layout.fragment_answer_wrod;
    }

    @Override
    public void initView(ViewGroup parent) {
        initData();
        initVoice();
        initToast();
    }

    public void initToast() {
        progressToast = new Toast(mContext);
        progressToast.setDuration(Toast.LENGTH_SHORT);
        TextView view = new TextView(mContext);
        progressToast.setView(view);
    }

    private void showProgress(final String str) {
        TextView view = (TextView) progressToast.getView();
        view.setText(str);
        progressToast.show();
    }

    /**
     * 开始语音评测阅读
     */
    public void beginSpeck() {
        if (mIse == null) {
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            ToastUtil.show(mContext, "创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        //格式 "[word]\napple\nbanana\norange
        String words[] = currentExam.getWords().split(",");
        String evaText = "[word]\n" + words[0] + "\n" + words[1] + "\n" + words[2];
        Logger.d(evaText);
        mLastResult = null;
        setParams();
        mIse.startEvaluating(evaText, null, mEvaluatorListener);
    }

    /**
     * 开始语音阅读提示
     */
    public void beginVoice() {
        if (null == mTts) {
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            ToastUtil.show(mContext, "创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        String text = currentExam.getWords();
        // 设置参数
        setParam();
        int code = mTts.startSpeaking(text, mTtsListener);
        if (code != ErrorCode.SUCCESS) {
            ToastUtil.show(mContext, "语音合成失败,错误码: " + code);
        }
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            showProgress("提示开始");
        }

        @Override
        public void onSpeakPaused() {
            showProgress("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showProgress("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
            showProgress(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            showProgress(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showProgress("提示结束");
            } else if (error != null) {
                showProgress(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    /**
     * 评测监听接口
     */
    private EvaluatorListener mEvaluatorListener = new EvaluatorListener() {

        @Override
        public void onResult(EvaluatorResult result, boolean isLast) {
            if (isLast) {
                StringBuilder builder = new StringBuilder();
                builder.append(result.getResultString());

                if (!TextUtils.isEmpty(builder)) {
                    ((AnswerExamActivity) getActivity()).setWordResult(builder.toString());
                    Logger.d(builder.toString());
                }
                flSpeckVoice.setEnabled(true);
                mLastResult = builder.toString();
                showProgress("单词朗读完毕");
            }
        }

        @Override
        public void onError(SpeechError error) {
            flSpeckVoice.setEnabled(true);
            if (error != null) {
                showProgress("error:" + error.getErrorCode() + "," + error.getErrorDescription());
            } else {
            }
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Logger.d("evaluator begin");
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Logger.d("evaluator stoped");
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showProgress("当前音量：" + volume);
            Logger.d("返回音频数据：" + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }

    };

    /**
     * 语音合成参数设置
     *
     * @return
     */
    private void setParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, "10");
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, "50");
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, "80");
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            // 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");
        }
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }

    /**
     * 语音评测参数设置
     */
    private void setParams() {
        // 设置评测语言
        language = "en_us";
        // 设置需要评测的类型
        category = "read_word";//read_sentence
        // 设置结果等级（中文仅支持complete）
        result_level = "complete";
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        String vad_bos = "5000";
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        String vad_eos = "1800";
        // 语音输入超时时间，即用户最多可以连续说多长时间；
        String speech_timeout = "-1";

        mIse.setParameter(SpeechConstant.LANGUAGE, language);
        mIse.setParameter(SpeechConstant.ISE_CATEGORY, category);
        mIse.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        mIse.setParameter(SpeechConstant.VAD_BOS, vad_bos);
        mIse.setParameter(SpeechConstant.VAD_EOS, vad_eos);
        mIse.setParameter(SpeechConstant.KEY_SPEECH_TIMEOUT, speech_timeout);
        mIse.setParameter(SpeechConstant.RESULT_LEVEL, result_level);
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIse.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIse.setParameter(SpeechConstant.ISE_AUDIO_PATH, Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/ise.wav");
    }

    public void initVoice() {
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(mContext, mTtsInitListener);
        mIse = SpeechEvaluator.createEvaluator(mContext, null);
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                ToastUtil.show(mContext, "初始化失败,错误码：" + code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    /**
     * 试题请求的回调
     */
    public void initData() {
        currentTest = ((AnswerExamActivity) getActivity()).getCurrentTest();
        if (null != currentTest) {
            JsonEntity entity = StuDbUtils.getExamDetail(currentTest.getExamId());
            if (entity.getCode() == 0) {
                currentExam = (Exam) entity.getData();
                if (null != currentExam) {
                    String words[] = currentExam.getWords().split(",");
                    for (int i = 0; i < words.length; i++) {
                        queryWord(words[i], i);
                    }

                } else {
                    ToastUtil.show(mContext, "获取试题失败！");
                    getActivity().finish();
                }
            } else {
                ToastUtil.show(mContext, entity.getMsg());
            }
        } else {
            ToastUtil.show(mContext, "获取试题失败！");
            getActivity().finish();
        }
    }

    /**
     * 单词翻译
     *
     * @param inputWord 单词
     * @param what      序号
     */
    private void queryWord(String inputWord, final int what) {
        // 源语言或者目标语言其中之一必须为中文,目前只支持中文与其他几个语种的互译
        Language langFrom = LanguageUtils.getLangByName("英文");
        // 若设置为自动，则查询自动识别源语言，自动识别不能保证完全正确，最好传源语言类型
        // Language langFrosm = LanguageUtils.getLangByName("自动");
        Language langTo = LanguageUtils.getLangByName("中文");
        TranslateParameters tps = new TranslateParameters.Builder()
                .source("youdao").from(langFrom).to(langTo).build();// appkey可以省略
        Translator translator = Translator.getInstance(tps);
        translator.lookup(inputWord, new TranslateListener() {
            @Override
            public void onResult(Translate result, String input) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("result", result);
                Message message = new Message();
                message.what = what;
                message.setData(bundle);
                handler.sendMessage(message);
                //异步翻译结果，需要填充到页面
            }

            @Override
            public void onError(TranslateErrorCode error) {
                ToastUtil.show(mContext, "查询单词错误:" + error.name());
            }
        });
    }


    @OnClick({R.id.fl_speck_voice, R.id.fl_remind_voice})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.fl_speck_voice:
                beginSpeck();
                flSpeckVoice.setEnabled(false);
                break;
            case R.id.fl_remind_voice:
                beginVoice();
                break;
        }
    }
}
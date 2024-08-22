package com.likeminds.chatmm.search.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.databinding.LmChatActivitySearchBinding
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity

class LMChatSearchActivity : BaseAppCompatActivity() {

    private lateinit var binding: LmChatActivitySearchBinding

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LMChatSearchActivity::class.java)
            context.startActivity(intent)
        }

        fun getIntent(context: Context): Intent {
            return Intent(context, LMChatSearchActivity::class.java)
        }
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().searchComponent()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        SDKApplication.getInstance().searchComponent()?.inject(this)
        super.onCreate(savedInstanceState)
        binding = LmChatActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
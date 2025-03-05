package com.likeminds.chatmm.report.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.likeminds.chatmm.R
import com.likeminds.chatmm.databinding.ActivityReportBinding
import com.likeminds.chatmm.report.model.ReportExtras
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity

class ReportActivity : BaseAppCompatActivity() {

    private lateinit var binding: ActivityReportBinding

    companion object {
        const val REPORT_EXTRAS = "REPORT_EXTRAS"

        @JvmStatic
        fun start(context: Context, extras: ReportExtras) {
            val intent = Intent(context, ReportActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(REPORT_EXTRAS, extras)
            intent.putExtra("bundle", bundle)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getIntent(context: Context, extras: ReportExtras): Intent {
            val intent = Intent(context, ReportActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(REPORT_EXTRAS, extras)
            intent.putExtra("bundle", bundle)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Call before the DecorView is accessed in setContentView
        theme.applyStyle(R.style.OptOutEdgeToEdgeEnforcement, /* force */ false)

        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
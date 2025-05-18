package com.likeminds.chatmm.report.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        binding = ActivityReportBinding.inflate(layoutInflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { view, windowInsets ->
            val innerPadding = windowInsets.getInsets(
                // Notice we're using systemBars, not statusBar
                WindowInsetsCompat.Type.systemBars()
                        // Notice we're also accounting for the display cutouts
                        or WindowInsetsCompat.Type.displayCutout()
                        // If using EditText, also add
                        or WindowInsetsCompat.Type.ime()
            )
            // Apply the insets as padding to the view. Here, set all the dimensions
            // as appropriate to your layout. You can also update the view's margin if
            // more appropriate.
            view.setPadding(0, innerPadding.top, 0, innerPadding.bottom)

            // Return CONSUMED if you don't want the window insets to keep passing down
            // to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        setContentView(binding.root)
    }
}
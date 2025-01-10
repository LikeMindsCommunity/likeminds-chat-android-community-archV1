package com.likeminds.community.hybrid.chat.auth.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.likeminds.chatsampleapp.databinding.FragmentUserBinding

class UserFragment : Fragment() {

    private lateinit var binding: FragmentUserBinding

    companion object {

        @JvmStatic
        fun getInstance(): UserFragment {
            return UserFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnLogout.setOnClickListener {
            // todo: logout
//            LikeMinds.logout(requireContext()) {
//                Toast.makeText(requireContext(), "${it.success}", Toast.LENGTH_SHORT).show()
//            }
        }
    }
}
package com.geardex.app.ui.glovebox

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.geardex.app.R
import com.geardex.app.databinding.FragmentGloveboxBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GloveboxFragment : Fragment() {

    private var _binding: FragmentGloveboxBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GloveboxViewModel by viewModels()
    private lateinit var adapter: DocumentAdapter

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGloveboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DocumentAdapter(
            onOpenClick = { doc ->
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { viewModel.prepareDocumentForViewing(doc) }
                        .onSuccess { file ->
                            val uri = FileProvider.getUriForFile(
                                requireContext(), "${requireContext().packageName}.provider", file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, requireContext().contentResolver.getType(uri) ?: "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(Intent.createChooser(intent, doc.fileName))
                        }
                        .onFailure {
                            Toast.makeText(requireContext(), R.string.doc_open_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            onDeleteClick = { doc -> viewModel.deleteDocument(doc) }
        )

        binding.recyclerDocuments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GloveboxFragment.adapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_slide_up)
        }

        binding.fabAddDocument.setOnClickListener {
            findNavController().navigate(R.id.action_glovebox_to_addDocument)
        }

        binding.btnExportZip.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val zipFile = viewModel.exportZip()
                val uri = FileProvider.getUriForFile(
                    requireContext(), "${requireContext().packageName}.provider", zipFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                exportLauncher.launch(Intent.createChooser(shareIntent, getString(R.string.doc_export_zip)))
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.documents.collect { docs ->
                    adapter.submitList(docs)
                    binding.layoutEmpty.visibility = if (docs.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerDocuments.visibility = if (docs.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

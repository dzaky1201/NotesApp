package com.dzakyhdr.notes.ui.list

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.dzakyhdr.notes.R
import com.dzakyhdr.notes.adapter.ListAdapter
import com.dzakyhdr.notes.databinding.FragmentListBinding
import com.dzakyhdr.notes.model.ToDoData
import com.dzakyhdr.notes.ui.add.SwipeToDelete
import com.dzakyhdr.notes.utils.hideKeyBoard
import com.dzakyhdr.notes.viewmodel.SharedViewModel
import com.dzakyhdr.notes.viewmodel.ToDoViewModel
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.animators.LandingAnimator


class ListFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val toDoViewModel: ToDoViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by viewModels()

    private val adapter: ListAdapter by lazy { ListAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list, container, false)
        binding.lifecycleOwner = this
        binding.sharedViewModel = sharedViewModel

        toDoViewModel.getAllData.observe(viewLifecycleOwner, Observer { data ->
            sharedViewModel.checkDatabaseEmpty(data)
            adapter.setData(data)
        })
        setupRecyclerView()
        hideKeyBoard(requireActivity())
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.list_fragment_menu, menu)

        val search = menu.findItem(R.id.menu_search)
        val searchView = search.actionView as? SearchView
        searchView?.isSubmitButtonEnabled = true
        searchView?.setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_delete_all -> confirmRemoval()
            R.id.menu_priority_high -> {toDoViewModel.sortByHighPriority.observe(this, Observer {
                adapter.setData(it)
            })}
            R.id.menu_priority_low -> {toDoViewModel.sortByLowPriority.observe(this, Observer {
                adapter.setData(it)
            })}
        }

        return super.onOptionsItemSelected(item)
    }

    private fun confirmRemoval() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus")
            .setMessage("Apakah Yakin Ingin menghapus semua catatan ?")
            .setPositiveButton("Yes") { _, _ ->
                toDoViewModel.deleteAllData()
                Toast.makeText(requireContext(), "Berhasil Dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .create()
            .show()
    }

    private fun setupRecyclerView() {
        val rv = binding.rvTodo
        rv.adapter = adapter
        rv.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        rv.itemAnimator = LandingAnimator().apply {
            addDuration = 300
        }

        //swipe delete
        swipeToDelete(rv)
    }

    private fun swipeToDelete(rv: RecyclerView) {
        val swipeDeleteCallback = object : SwipeToDelete(){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val delete = adapter.dataList[viewHolder.adapterPosition]

                //delete item
                toDoViewModel.deleteData(delete)
                adapter.notifyItemRemoved(viewHolder.adapterPosition)

                //restore delete item
                restoreDeleteData(viewHolder.itemView, delete)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeDeleteCallback)
        itemTouchHelper.attachToRecyclerView(rv)
    }

    private fun restoreDeleteData(view: View, delete: ToDoData) {
        val snackbar = Snackbar.make(
            view,
            "Delete: ${delete.title}",
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction("undo"){
            toDoViewModel.insertData(delete)
        }

        snackbar.show()
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null){
            searchData(query)
        }

        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null) {
            searchData(query)
        }
        return true
    }

    private fun searchData(query: String) {
        val searchQuery = "%$query%"

        toDoViewModel.searchDatabase(searchQuery).observe(viewLifecycleOwner, Observer { list ->
            list?.let {
                adapter.setData(it)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
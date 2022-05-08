package com.s16.smluncher.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.s16.smluncher.MainApp
import com.s16.smluncher.R
import com.s16.smluncher.adapters.DrawerAdapter
import com.s16.smluncher.managers.AppInfo
import com.s16.view.GridAutoFitLayoutManager
import com.s16.view.RecyclerViewArrayAdapter
import com.s16.view.SearchEditTextHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DrawerFragment : Fragment(), SearchEditTextHelper.OnSearchListener, PopupMenu.OnMenuItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var drawerAdapter: DrawerAdapter
    private lateinit var searchHelper: SearchEditTextHelper
    private lateinit var mPopupMenu: PopupMenu

    private val mainApp by lazy { requireActivity().application as MainApp }

    private var uiScope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var searchView : EditText
        var actionMenu: View

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_drawer, container, false).apply {
            recyclerView = this.findViewById(R.id.recyclerView)
            searchView = this.findViewById(R.id.searchBarView)
            actionMenu = this.findViewById(R.id.actionMenu)
        }

        searchHelper = SearchEditTextHelper(searchView)
        searchHelper.setOnSearchListener(this)

        drawerAdapter = DrawerAdapter(requireContext())
        drawerAdapter.setOnItemClickListener(object: RecyclerViewArrayAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                drawerAdapter.getItem(position)?.let { item ->
                    onItemClick(view, item)
                }
            }
        })

        recyclerView.apply {
            layoutManager = GridAutoFitLayoutManager(requireContext(), 100)
            adapter = drawerAdapter
        }

        actionMenu.setOnClickListener {
            showPopupMenu(it)
        }

        mainApp.preferences.hideApps.observe(requireActivity()) {
            drawerAdapter.setHideApps(it.toList())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainApp.apps.observe(viewLifecycleOwner) {
            job = uiScope.launch {
                drawerAdapter.submitList(it)
            }
        }
    }

    private fun onItemClick(view: View, app: AppInfo) {
        requireActivity().onBackPressed()
        app.launchApp(requireContext())
    }

    override fun onSearchTextChanged(searchText: CharSequence?) {
        searchText?.let {
            drawerAdapter.filter.filter(it)
        }
    }

    override fun onSearchSubmit(searchText: CharSequence?) {
    }

    private fun showPopupMenu(view: View) {
        if (!this::mPopupMenu.isInitialized) {
            mPopupMenu = PopupMenu(requireContext(), view)
            mPopupMenu.inflate(R.menu.main)
            mPopupMenu.setOnMenuItemClickListener(this)
        }
        mPopupMenu.show()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean = item?.let {
        requireActivity().onOptionsItemSelected(item)
    } ?: false

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
}
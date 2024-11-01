package com.netmontools.lookatnet.ui.local.view;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.netmontools.lookatnet.App;
import com.netmontools.lookatnet.MainViewModel;
import com.netmontools.lookatnet.R;
import com.netmontools.lookatnet.ui.local.LocalRepository;
import com.netmontools.lookatnet.ui.local.model.Folder;
import com.netmontools.lookatnet.ui.local.viewmodel.LocalViewModel;
import com.netmontools.lookatnet.utils.SimpleUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class  LocalFragment extends Fragment {

    private static LocalViewModel localViewModel;
    private static MainViewModel mainViewModel;
    private SwipeRefreshLayout localRefreshLayout;
    private RecyclerView recyclerView;
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(App.instance);
    AppCompatActivity appCompatActivity;
    ActionBar appBar;
    AutoFitGridLayoutManager layoutManager;
    private static LocalAdapter adapter;
    private static int position;
    @SuppressLint("UseSparseArrays")
    SparseArray<Boolean> selectedArray = new SparseArray<>();
    boolean isSelected, isListMode;

    public LocalFragment() {
    }

    public static LocalFragment newInstance(int index) {
        return new LocalFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = ViewModelProviders.of((FragmentActivity) requireContext()).get(MainViewModel.class);
        String actionBarTitle = App.rootPath;
        sp.edit().putString("root_path", actionBarTitle).apply();
        //mainViewModel.updateActionBarTitle(actionBarTitle);

        appCompatActivity = (AppCompatActivity)getActivity();
        assert appCompatActivity != null;
        appBar = appCompatActivity.getSupportActionBar();
        assert appBar != null;
        appBar.setTitle(actionBarTitle);;
        setHasOptionsMenu(true);
        if(savedInstanceState != null) {
           int mode = savedInstanceState.getInt("mode");
            if(mode == 0) {
                isListMode = false;
            } else {
                isListMode = true;
            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_local, container, false);
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        OnBackPressedCallback callback = new OnBackPressedCallback(
                true // default to enabled
        ) {
            @Override
            public void handleOnBackPressed() {
                try {
                    File file = new File(LocalRepository.previousPath);
                    if (!file.getPath().equalsIgnoreCase(LocalRepository.rootPath)) {
                        if (file.exists()) {
                            file = new File(Objects.requireNonNull(file.getParent()));
                            Folder fd = new Folder();
                            fd.isFile = file.isFile();
                            fd.setName(file.getName());
                            fd.setPath(file.getPath());
                            if (fd.isFile) {
                                fd.setSize(file.length());
                                fd.setImage(LocalRepository.file_image);
                            } else {
                                fd.setSize(0L);
                                fd.setImage(LocalRepository.folder_image);
                            }
                            localViewModel.update(fd);
                            localRefreshLayout.setRefreshing(true);
                            mainViewModel.updateActionBarTitle(file.getName());
                        }
                    } else {
                        this.setEnabled(false);
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    }

                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(
                this, // LifecycleOwner
                callback);
    }

    public class AutoFitGridLayoutManager extends GridLayoutManager {

        private int columnWidth;
        private boolean columnWidthChanged = true;

        public AutoFitGridLayoutManager(Context context, int columnWidth) {
            super(context, 1);

            setColumnWidth(columnWidth);
        }

        public void setColumnWidth(int newColumnWidth) {
            if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
                columnWidth = newColumnWidth;
                columnWidthChanged = true;
            }
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            if (columnWidthChanged && columnWidth > 0) {
                int totalSpace;
                if (getOrientation() == VERTICAL) {
                    totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
                } else {
                    totalSpace = getHeight() - getPaddingTop() - getPaddingBottom();
                }
                int spanCount = Math.max(1, totalSpace / columnWidth);
                setSpanCount(spanCount);
                columnWidthChanged = true;
            }
            super.onLayoutChildren(recycler, state);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        localRefreshLayout = view.findViewById(R.id.local_refresh_layout);
        localRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);
        localRefreshLayout.setEnabled(false);


        recyclerView = view.findViewById(R.id.local_recycler_view);
        //recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        layoutManager = new AutoFitGridLayoutManager(getActivity(), 400);
        recyclerView.setLayoutManager(layoutManager);
        isListMode = false;
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);

        adapter = new LocalAdapter();
        recyclerView.setAdapter(adapter);

        localViewModel = new ViewModelProvider.AndroidViewModelFactory(App.getInstance()).create(LocalViewModel.class);
        localViewModel.getAllPoints().observe(getViewLifecycleOwner(), new Observer<List<Folder>>() {
            @Override
            public void onChanged(List<Folder> points) {
                adapter.setPoints(points);
                localRefreshLayout.setRefreshing(false);
            }
        });

        adapter.setOnItemClickListener(new LocalAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Folder point) {
                isSelected = false;
                for (Folder folder : point.folders) {
                    if (folder.isChecked) {
                        if (point.isChecked) {
                            point.isChecked = false;
                        } else {
                            point.isChecked = true;
                        }
                        isSelected = true;
                        selectedArray.put(position, point.isChecked);
                        adapter.notifyItemChanged(position);
                        break;
                    }
                }

                if (!point.isFile) {
                    localViewModel.update(point);
                    localRefreshLayout.setRefreshing(true);
                    if (point.getName() != null) {
                        String pointName = point.getName();
                        mainViewModel.updateActionBarTitle(pointName);
                    }
                } else {
                    try {
                        assert point.getPath() != null;
                        File file = new File(point.getPath());
                        if (file.exists() && (file.isFile())) {
                            String ext = SimpleUtils.getExtension(file.getName());
                            if (ext.equals("jpg") || (ext.equals("jpeg")
                                    || (ext.equals("bmp") || (ext.equals("png"))))) {
                                NavController navController =
                                        Navigation.findNavController(requireActivity(),
                                                R.id.nav_host_fragment);

                                Bundle bundle= new Bundle();
                                bundle.putString("arg", file.getPath());
                                navController.navigate(R.id.action_nav_local_to_nav_image, bundle);
                            }else if (ext.equalsIgnoreCase("fb2")) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setType("*/*");
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Intent chosenIntent = Intent.createChooser(intent, "Choose file...");
                                startActivity(chosenIntent);
                            } else {
                                SimpleUtils.openFile(App.instance, file);
                            }
                        }
                    } catch (NullPointerException npe) {
                        npe.getMessage();
                    }
                }
            }
        });

        adapter.setOnItemLongClickListener(new LocalAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(Folder point) {
                if (point.isChecked) {
                    point.isChecked = false;
                } else {
                    point.isChecked = true;
                }
                //localViewModel.update(adapter.getPointAt(position));
                adapter.notifyItemChanged(position);
            }
        });
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int mode = 0;
        if(isListMode) {
            mode = 1;
        }

        outState.putInt("mode", mode);
    }

    @Override
    public void onPause() {
        super.onPause();
        //String actionBarTitle = Objects.requireNonNull(appBar.getTitle()).toString();
        //sp.edit().putString("actionbar_title", actionBarTitle).apply();
        //sp.edit().putBoolean("layout_mode", isListMode).apply()
    }
    @Override
    public void onResume() {
        super.onResume();
        String actionBarTitle = sp.getString("actionbar_title", "");
        if(actionBarTitle.equalsIgnoreCase("0")) {
            mainViewModel.updateActionBarTitle(LocalRepository.rootPath);
        } else mainViewModel.updateActionBarTitle(actionBarTitle);

//        if (isListMode == false) {
//            //recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
//            recyclerView.setLayoutManager(layoutManager);
//            //item.setIcon(R.drawable.baseline_view_list_yellow_24);
//        } else {
//            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//            //item.setIcon(R.drawable.baseline_view_column_yellow_24);
//        }
    }
    private void deleteFolder() {
        confirmDelete.instantiate().show(requireActivity().getSupportFragmentManager(), "confirm delete");
    }

    public static class confirmDelete extends DialogFragment {

        private static DialogFragment instantiate() { return new confirmDelete();
        }

        @NotNull
        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(R.string.confirm_title);
            builder.setMessage(R.string.confirm_message);
            builder.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int button) {
                            localViewModel.delete(adapter.getPointAt(position));
                            Toast.makeText(getActivity(), "File object deleted", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
            builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int button) {
                            adapter.notifyItemChanged(position);
        }
    }
            );
            return builder.create();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_local, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if (!isListMode) {
            menu.findItem(R.id.listMode).setIcon(R.drawable.baseline_view_list_yellow_24);
        } else {
            menu.findItem(R.id.listMode).setIcon(R.drawable.baseline_view_column_yellow_24);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.property: {
                if (selectedArray.size() > 0) {

                }
                    return true;
            }
            case R.id.listMode: {
                if (isListMode == false) {
                    isListMode = true;
                    //recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
                    recyclerView.setLayoutManager(layoutManager);
                    item.setIcon(R.drawable.baseline_view_list_yellow_24);
                } else {
                    isListMode = false;
                    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                    item.setIcon(R.drawable.baseline_view_column_yellow_24);
                }
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
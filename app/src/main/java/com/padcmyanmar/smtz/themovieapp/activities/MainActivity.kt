package com.padcmyanmar.smtz.themovieapp.activities

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.padcmyanmar.smtz.themovieapp.R
import com.padcmyanmar.smtz.themovieapp.adapters.BannerAdapter
import com.padcmyanmar.smtz.themovieapp.adapters.ShowcaseAdapter
import com.padcmyanmar.smtz.themovieapp.data.vos.GenreVO
import com.padcmyanmar.smtz.themovieapp.data.vos.MovieVO
import com.padcmyanmar.smtz.themovieapp.delegate.BannerViewHolderDelegate
import com.padcmyanmar.smtz.themovieapp.delegate.MovieViewHolderDelegate
import com.padcmyanmar.smtz.themovieapp.delegate.ShowcaseViewHolderDelegate
import com.padcmyanmar.smtz.themovieapp.mvvm.MainViewModel
import com.padcmyanmar.smtz.themovieapp.utils.IMAGE_BASE_URL
import com.padcmyanmar.smtz.themovieapp.utils.PREVIOUS_MOVIE
import com.padcmyanmar.smtz.themovieapp.utils.defaultPrefs
import com.padcmyanmar.smtz.themovieapp.utils.get
import com.padcmyanmar.smtz.themovieapp.utils.observeOnce
import com.padcmyanmar.smtz.themovieapp.utils.set
import com.padcmyanmar.smtz.themovieapp.viewPods.ActorListViewPod
import com.padcmyanmar.smtz.themovieapp.viewPods.MovieListViewPod
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.navigation_drawer.navView
import kotlinx.android.synthetic.main.navigation_drawer.view.layoutPreviouslyWatched
import kotlinx.android.synthetic.main.navigation_drawer.view.navView
import kotlinx.android.synthetic.main.navigation_drawer.view.tvDeveloperTag
import kotlinx.android.synthetic.main.navigation_drawer.view.tvHeading
import kotlinx.android.synthetic.main.view_holder_movie_list.view.ivMovieImage
import kotlinx.android.synthetic.main.view_holder_movie_list.view.rbMovieRating
import kotlinx.android.synthetic.main.view_holder_movie_list.view.tvMovieName
import kotlinx.android.synthetic.main.view_holder_movie_list.view.tvMovieRating

class MainActivity : AppCompatActivity(), BannerViewHolderDelegate, ShowcaseViewHolderDelegate,
    MovieViewHolderDelegate {

    lateinit var mBannerAdapter: BannerAdapter
    lateinit var mShowcaseAdapter: ShowcaseAdapter

    lateinit var mBestPopularMovieViewPod: MovieListViewPod
    lateinit var mGenreMovieViewPod: MovieListViewPod
    lateinit var mActorListViewPod: ActorListViewPod

    private lateinit var mMainViewModel: MainViewModel

    private val previousMovieIdLiveData = MutableLiveData<Int>()

    private val viewHolderItem by lazy { drawerLayout.navView.layoutPreviouslyWatched }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpViewModel()

        setUpToolBar()
        setUpBanner()
        setUpListeners()
        setUpShowCase()
        setUpViewPods()
        setUpDrawer()

        observeLiveData()
    }

    private fun setUpViewModel() {
        mMainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mMainViewModel.getInitialData()
    }

    private fun observeLiveData() {

//        mMainViewModel.nowPlayingMoviesLiveData?.observe(this){
//            mBannerAdapter.setNewData(it)
//        }
        mMainViewModel.nowPlayingMoviesLiveData?.observe(this, mBannerAdapter::setNewData)
        mMainViewModel.popularMoviesLiveData?.observe(this, mBestPopularMovieViewPod::setData)
        mMainViewModel.topRatedMoviesLiveData?.observe(this, mShowcaseAdapter::setNewData)
        mMainViewModel.genresLiveData.observe(this, this::setUpGenreTabLayout)
        mMainViewModel.actorsLiveData.observe(this, mActorListViewPod::setData)
        mMainViewModel.moviesByGenreLiveData.observe(this, mGenreMovieViewPod::setData)

        // Manually trigger the initial value retrieval
        previousMovieIdLiveData.postValue(defaultPrefs(this)[PREVIOUS_MOVIE, 0])

        previousMovieIdLiveData.observe(this) { movieId ->
            bindNavData(movieId)
        }
    }

    // for navigation recently watched
    private fun bindNavData(movieId: Int) {

        // Handle the real-time updated value
        if (movieId == 0) {
            navView.tvHeading.text = "There is no recently watched movie."
            viewHolderItem.visibility = View.GONE
        } else {
            navView.tvHeading.text = "Recently watched Movie"
            viewHolderItem.visibility = View.VISIBLE
            mMainViewModel.getMovieById(movieId)

            // to avoid double observing observe for one time
            mMainViewModel.movieDetailsLiveData?.observeOnce(this) {
                it?.let { movie ->
                    viewHolderItem.ivMovieImage
                    Glide.with(applicationContext)
                        .load("$IMAGE_BASE_URL${movie.posterPath}")
                        .into(viewHolderItem.ivMovieImage)

                    viewHolderItem.tvMovieName.text = movie.title
                    viewHolderItem.tvMovieRating.text = movie.voteAverage?.toString()
                    viewHolderItem.rbMovieRating.rating = movie.getRatingBasedOnFiveStars()
                }
            }
        }
    }

    //1  ViewPod instances
    private fun setUpViewPods() {
        mBestPopularMovieViewPod = vpBestPopularMovieList as MovieListViewPod
        mBestPopularMovieViewPod.setUpMovieListViewPod(this)

        mGenreMovieViewPod = vpGenreMovieList as MovieListViewPod
        mGenreMovieViewPod.setUpMovieListViewPod(this)

        //Module12 ep11
        mActorListViewPod = vpActorsHomeScreen as ActorListViewPod
    }

    private fun setUpShowCase() {
        mShowcaseAdapter = ShowcaseAdapter(this)
        rvShowcases.adapter = mShowcaseAdapter
        rvShowcases.layoutManager =
            LinearLayoutManager(applicationContext, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setUpListeners() {

        // Handle the click event for the menu icon
        toolBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        //Genre Tab Layout
        tabLayoutGenre.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                mMainViewModel.getMoviesByGenre(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    //Tab Bar
    private fun setUpGenreTabLayout(genreList: List<GenreVO>) {

        genreList.forEach {
            tabLayoutGenre.newTab().apply {
                text = it.name
                tabLayoutGenre.addTab(this)
            }
        }
    }

    //Banner Adapter
    private fun setUpBanner() {

        mBannerAdapter = BannerAdapter(this)
        viewPagerBanner.adapter = mBannerAdapter

        //Dots Indicator
        dotsIndicatorBanner.attachTo(viewPagerBanner)
    }

    //Search icon
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_discover, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.movie_search) {
            val intent = Intent(this, MovieSearchActivity::class.java)
            startActivity(intent)
            return true
        }
        return false
    }

    //App Bar Leading icon
    private fun setUpToolBar() {
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
    }

    private fun setUpDrawer() {

        val navView = drawerLayout.navView
        navView.tvDeveloperTag.paintFlags = Paint.UNDERLINE_TEXT_FLAG

    }

    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) ->
                drawerLayout.closeDrawer(GravityCompat.START)
            else -> {
//                showInterstitialAd(BACK_PRESSED)
                super.onBackPressed()
            }
        }
    }

    override fun onTapMovieFormBanner(movieId: Int) {
        previousMovieIdLiveData.postValue(movieId)
        defaultPrefs(this).set(PREVIOUS_MOVIE, movieId)
        startActivity(MovieDetailsActivity.newIntent(this, movieId))
    }

    override fun onTapMovieFromShowcase(movieId: Int) {
        previousMovieIdLiveData.postValue(movieId)
        defaultPrefs(this)[PREVIOUS_MOVIE] = movieId
        startActivity(MovieDetailsActivity.newIntent(this, movieId))
    }

    override fun onTapMovie(movieId: Int) {
        previousMovieIdLiveData.postValue(movieId)
        defaultPrefs(this)[PREVIOUS_MOVIE] = movieId
        startActivity(MovieDetailsActivity.newIntent(this, movieId))
    }
}
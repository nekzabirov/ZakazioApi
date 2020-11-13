package com.zakaion.api.dao

import com.zakaion.api.entity.FeedbackEntity
import org.springframework.data.repository.PagingAndSortingRepository

interface FeedbackDao : PagingAndSortingRepository<FeedbackEntity, Long>
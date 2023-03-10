package Mofit.com.api.service;

import Mofit.com.Domain.Rank;
import Mofit.com.api.request.GameEndReq;
import Mofit.com.exception.EntityNotFoundException;
import Mofit.com.repository.MemberRepository;
import Mofit.com.repository.RankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RankingService{

    private final RankRepository rankRepository;

    private CacheManager cacheManager;

    public Rank getRankById(String userId) {
        return rankRepository.findById(userId).orElse(null);
    }


    @CachePut(value ="user_rank",cacheManager = "myCacheManager")
    public void updateRankWin(String winId, String userId) {
        Rank id = getRankById(userId);
        if (id == null){
            throw new EntityNotFoundException("존재하지 않는 Id 입니다");
        }
        if(Objects.equals(winId, id.getId())){
            id.setWin(id.getWin() + 1);
        }
        id.setGames(id.getGames()+1);

        rankRepository.save(id);

    }

    @CachePut(value ="user_score",key = "request.userId",cacheManager = "myCacheManager")
    public Rank updateRankScore(GameEndReq request) {

        Rank user = getRankById(request.getUserId());

        double value = Double.parseDouble(request.getScore());

        if (user.getScore() == 0) {
            user.setScore(value);

        } else if (value >= user.getScore()) {
            return user;
        }

        user.setScore(value);
        rankRepository.save(user);

        return user;
    }


    @Cacheable(value ="user_rank", cacheManager = "myCacheManager")
    public List<Rank> rankingList() {
        return rankRepository.findAll();
    }

    @Cacheable(value ="user_score", cacheManager = "myCacheManager")
    public List<Rank> rankingListScore() {
        Cache cache = cacheManager.getCache("user_score");
        Cache.ValueWrapper valueWrapper = cache.get("user_score_list");
        if (valueWrapper != null) {
            return (List<Rank>) valueWrapper.get();
        }
        List<Rank> ranks = rankRepository.findNonZeroScoreRecords();
        cache.put("user_score_list", ranks);
        return ranks;
    }



}

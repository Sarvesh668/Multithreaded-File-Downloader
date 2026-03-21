package com.example.filedowloader.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.filedowloader.demo.model.DownloadChunk;

@Repository
public interface ChunkRepository extends JpaRepository<DownloadChunk, Long> {

}

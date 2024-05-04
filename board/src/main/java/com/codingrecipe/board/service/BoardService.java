package com.codingrecipe.board.service;

import com.codingrecipe.board.dto.BoardDTO;
import com.codingrecipe.board.entity.BoardEntity;
import com.codingrecipe.board.entity.BoardFileEntity;
import com.codingrecipe.board.repository.BoardFileRepository;
import com.codingrecipe.board.repository.BoardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository repository;
    private final BoardFileRepository boardFileRepository;

    public String save(BoardDTO boardDTO) throws IOException {
        if (boardDTO.getBoardFile().isEmpty()){
            //첨부파일이 없는 경우
            BoardEntity boardEntity = BoardEntity.toSaveEntity(boardDTO);
            repository.save(boardEntity);
        } else {
            /*
                1. DTO에 담긴 파일을 꺼냄
                2. 파일의 이름 가져옴
                3. 서버 저장용 이름을 만듦
                // 내사진.jpg => 839798375892_내사진.jpg
                4. 저장 경로 설정
                5. 해당 경로에 파일 저장
                6. board_table에 해당 데이터 save 처리
                7. board_file_table에 해당 데이터 save 처리
             */
            BoardEntity boardEntity = BoardEntity.tosaveFileEntity(boardDTO);
            Long savedId = repository.save(boardEntity).getId();
            BoardEntity board = repository.findById(savedId).get();

            for (MultipartFile boardFile : boardDTO.getBoardFile()) {
//            MultipartFile boardFile = boardDTO.getBoardFile();
                String originalFileName = boardFile.getOriginalFilename();
                String storedFileName = System.currentTimeMillis() + "_" + originalFileName;
                String savePath = "C:\\board_img" + storedFileName;
                boardFile.transferTo(new File(savePath));

                BoardFileEntity boardFileEntity = BoardFileEntity.toBoardFileEntity(board, originalFileName, storedFileName);
                boardFileRepository.save(boardFileEntity);
            }
        }

        return "index";
    }

    public List<BoardDTO> findAll() {
        List<BoardEntity> entityList = repository.findAll();
        List<BoardDTO> boardDTOList = new ArrayList<>();

        for (BoardEntity boardEntity : entityList){
            boardDTOList.add(BoardDTO.toBoardDTO(boardEntity));
        }

        return boardDTOList;
    }

    @Transactional
    public void updateHits(Long id){
        repository.updateHits(id);
    }

    @Transactional
    public BoardDTO findById(Long id) {
        Optional<BoardEntity> findEntity = repository.findById(id);
        if (findEntity.isPresent()){
            BoardEntity entity = findEntity.get();
            BoardDTO boardDTO = BoardDTO.toBoardDTO(entity);
            return boardDTO;
        } else {
            return null;
        }
    }

    public BoardDTO update(BoardDTO boardDTO) {
        BoardEntity boardEntity = BoardEntity.toUpdateEntity(boardDTO);
        repository.save(boardEntity);
        return findById(boardDTO.getId());
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<BoardDTO> paging(Pageable pageable){
        int page = pageable.getPageNumber() - 1;
        int pageLimit = 3;

        Page<BoardEntity> boardEntityPage = repository.findAll(PageRequest.of(page, pageLimit, Sort.by(Sort.Direction.DESC, "id")));

        System.out.println("boardEntityPage.getContent() = " + boardEntityPage.getContent()); // 요청 페이지에 해당하는 글
        System.out.println("boardEntityPage.getTotalElements() = " + boardEntityPage.getTotalElements()); // 전체 글갯수
        System.out.println("boardEntityPage.getNumber() = " + boardEntityPage.getNumber()); // DB로 요청한 페이지 번호
        System.out.println("boardEntityPage.getTotalPages() = " + boardEntityPage.getTotalPages()); // 전체 페이지 갯수
        System.out.println("boardEntityPage.getSize() = " + boardEntityPage.getSize()); // 한 페이지에 보여지는 글 갯수
        System.out.println("boardEntityPage.hasPrevious() = " + boardEntityPage.hasPrevious()); // 이전 페이지 존재 여부
        System.out.println("boardEntityPage.isFirst() = " + boardEntityPage.isFirst()); // 첫 페이지 여부
        System.out.println("boardEntityPage.isLast() = " + boardEntityPage.isLast()); // 마지막 페이지 여부

        Page<BoardDTO> boardDTOS = boardEntityPage.map(board -> new BoardDTO(board.getId(), board.getBoardWriter(), board.getBoardTitle(), board.getBoardHits(), board.getCreatedTime()));
        return boardDTOS;
    }
}



package ci.orange.messagerie.dao.repository;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Locale;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ci.orange.messagerie.utils.*;
import ci.orange.messagerie.utils.dto.*;
import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.contract.Request;
import ci.orange.messagerie.utils.contract.Response;
import ci.orange.messagerie.dao.entity.*;
import ci.orange.messagerie.dao.repository.base._ParticipantConversationRepository;

/**
 * Repository : ParticipantConversation.
 *
 * @author Geo
 */
@Repository
public interface ParticipantConversationRepository extends JpaRepository<ParticipantConversation, Integer>, _ParticipantConversationRepository {


  @Query("SELECT DISTINCT pc1.conversation FROM ParticipantConversation pc1 " +
         "INNER JOIN ParticipantConversation pc2 ON pc1.conversation.id = pc2.conversation.id " +
         "WHERE pc1.user.id = :userId1 AND pc2.user.id = :userId2 " +
         "AND pc1.conversation.typeConversation.code = :typeConversationCode " +
         "AND pc1.isDeleted = :isDeleted AND pc2.isDeleted = :isDeleted " +
         "AND pc1.conversation.isDeleted = :isDeleted " +
         "AND (SELECT COUNT(p) FROM ParticipantConversation p WHERE p.conversation.id = pc1.conversation.id AND p.isDeleted = :isDeleted) = 2")
  Conversation findPrivateConversationBetweenUsers(
      @Param("userId1") Integer userId1,
      @Param("userId2") Integer userId2,
      @Param("typeConversationCode") String typeConversationCode,
      @Param("isDeleted") Boolean isDeleted);

  /**
   * Trouve tous les participants actifs d'une conversation (non supprimés et n'ayant pas quitté)
   * 
   * @param conversationId L'ID de la conversation
   * @return Liste des participants actifs
   */
  @Query("SELECT e FROM ParticipantConversation e WHERE e.conversation.id = :conversationId " +
         "AND e.isDeleted = false " +
         "AND (e.hasLeft = false OR e.hasLeft IS NULL) " +
         "AND (e.hasDefinitivelyLeft = false OR e.hasDefinitivelyLeft IS NULL)")
  List<ParticipantConversation> findActiveParticipantsByConversationId(@Param("conversationId") Integer conversationId);

  /**
   * Trouve le ParticipantConversation pour un utilisateur et une conversation spécifiques
   * 
   * @param userId L'ID de l'utilisateur
   * @param conversationId L'ID de la conversation
   * @param isDeleted Indique si on cherche les participants supprimés ou non
   * @return Le ParticipantConversation correspondant, ou null s'il n'existe pas
   */
  @Query("SELECT e FROM ParticipantConversation e WHERE e.user.id = :userId " +
         "AND e.conversation.id = :conversationId AND e.isDeleted = :isDeleted")
  ParticipantConversation findByUserIdAndConversationId(
      @Param("userId") Integer userId,
      @Param("conversationId") Integer conversationId,
      @Param("isDeleted") Boolean isDeleted);

}

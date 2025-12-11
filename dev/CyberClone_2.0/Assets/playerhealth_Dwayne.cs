using System.Collections;
using UnityEngine;


/// <summary> 
/// 
/// this is gonna be my simply designed health system for the playable character 
/// with player tag 'player' 
/// Riyaz dont be dumb and make sure you associate this with the same gameobject as isometric character controller 1 
/// 
/// </summary>
public class playerhealth_Dwayne : MonoBehaviour
{
    [Header("health adjustments")]
    [SerializeField]
    private int maxHP = 100;

    [SerializeField]
    private int currentHP;
    ///
    ///
    ///
    [Header("adjust state of invincibility")]
    [SerializeField]
    [Tooltip("The character needs a state where he doesnt get attacked in repetition, gives a space before attack")]
    private float invincibilityStateDuration = 0.5f;
    
    private float lastdmgTIme = -999f;
    ///
    ///
    ///
    [Header("Sprite flash when damaged")]
    [SerializeField]
    [Tooltip("Sprite flash when hp dmg occurs")]
    private bool flashDmg = true;

    [SerializeField]
    private float flashdurationDisplay = 0.12f;

    private SpriteRenderer spriteRenderer;
    private Color originalColor;
    ///
    ///
    ///
    [Header("DWAYNE | DEATH SETTINGS")]
    [SerializeField]
    [Tooltip("delay before loading death screen so death animation can play")]
    private float deathDelay = 2f;
    
    [SerializeField]
    [Tooltip("name of death/game over scene to load")]
    private string deathSceneName = "DeathScreen";
    
    [SerializeField]
    [Tooltip("disable player controls on death")]
    private bool disableControlsOnDeath = true;
    ///
    ///
    ///
    [Header("DWAYNE | AUDIO")]
    [SerializeField]
    [Tooltip("audio source for damage and death sounds")]
    private AudioSource playerAudioSrc;
    
    [SerializeField]
    [Tooltip("sounds when taking damage, can have variations")]
    private AudioClip[] damageSfx;
    
    [SerializeField]
    [Tooltip("sound when dying")]
    private AudioClip deathSfx;
    
    [SerializeField]
    [Tooltip("pitch variation for damage sounds")]
    private float damagePitchVar = 0.1f;
    ///
    ///
    ///
    [Header("DWAYNE | ANIMATION")]
    [SerializeField]
    [Tooltip("animator for death animation")]
    private Animator playerAnim;
    
    [SerializeField]
    [Tooltip("death animation trigger name")]
    private string deathTrigger = "Death";
    
    /// <summary>
    /// 
    /// 
    /// private state tracking
    /// 
    /// 
    /// </summary>
    private bool isDead = false;

    void Start()
    {
        currentHP = maxHP;
        spriteRenderer = GetComponent<SpriteRenderer>();

        if (spriteRenderer != null)
        {
            originalColor = spriteRenderer.color;
        }
        
        //auto finding audio source if i forgot to assign it
        if (playerAudioSrc == null)
        {
            playerAudioSrc = GetComponent<AudioSource>();
            if (playerAudioSrc == null)
            {
                playerAudioSrc = gameObject.AddComponent<AudioSource>();
            }
        }
        
        playerAudioSrc.playOnAwake = false;
        playerAudioSrc.spatialBlend = 0f; //2D sound
        
        //auto finding animator
        if (playerAnim == null)
        {
            playerAnim = GetComponent<Animator>();
        }
        
        Debug.Log($"[player health] initialized | hp: {currentHP}/{maxHP}");
    }

    public void TakeDamage(int damage)
    {
        if (isDead) return; //cant take damage if already dead
        
        if (Time.time < lastdmgTIme + invincibilityStateDuration)
        {
            return;  //edit if only the invincible state is a bit buggy in testing
        }

        lastdmgTIme = Time.time;
        currentHP -= damage;

        Debug.Log($"[player health] dwayne took some mighty damage! {damage} | hp now {currentHP}/{maxHP}");

        //playing damage sound
        PlayDamageSound();

        //now we are gonna do the visual feedback here
        if (flashDmg && spriteRenderer != null)
        {
            StartCoroutine(FlashSprite());
        }

        //then we need to check for death
        if (currentHP <= 0)
        {
            Dead();
        }
    }

    /// <summary>
    /// 
    /// playing random damage sound with pitch variation
    /// prevents repetitive audio
    /// 
    /// </summary>
    private void PlayDamageSound()
    {
        if (playerAudioSrc == null || damageSfx == null || damageSfx.Length == 0) return;
        
        AudioClip selectedClip = damageSfx[Random.Range(0, damageSfx.Length)];
        playerAudioSrc.pitch = 1f + Random.Range(-damagePitchVar, damagePitchVar);
        playerAudioSrc.PlayOneShot(selectedClip);
    }

    public void Heal(int amount)
    {
        if (isDead) return; //cant heal if dead
        
        currentHP = Mathf.Min(currentHP + amount, maxHP);
        Debug.Log($"[player health] dwayne has gained health back to {amount}! | hp = {currentHP}/{maxHP}");
    }

    /// <summary>
    /// 
    /// death sequence
    /// disabling controls, playing death animation/sound
    /// loading death screen after delay
    /// 
    /// </summary>
    void Dead()
    {
        if (isDead) return; //prevent multiple death triggers
        
        isDead = true;
        
        Debug.Log("[player health] DWAYNE DIED - initiating death sequence");

        //disabling player controls
        if (disableControlsOnDeath)
        {
            var controller = GetComponent<IsometricCharacterController1>();
            if (controller != null)
            {
                controller.StopMovement();
                controller.enabled = false;
            }
        }
        
        //playing death animation
        if (playerAnim != null && !string.IsNullOrEmpty(deathTrigger))
        {
            playerAnim.SetTrigger(deathTrigger);
            Debug.Log("[player health] death animation triggered");
        }
        
        //playing death sound
        if (playerAudioSrc != null && deathSfx != null)
        {
            playerAudioSrc.PlayOneShot(deathSfx);
            Debug.Log("[player health] death sound played");
        }
        
        //loading death screen after delay
        StartCoroutine(LoadDeathScreen());
    }
    
    /// <summary>
    /// 
    /// coroutine to load death screen after delay
    /// gives time for death animation and sound to play
    /// 
    /// </summary>
    private IEnumerator LoadDeathScreen()
    {
        Debug.Log($"[player health] waiting {deathDelay} seconds before loading death screen");
        
        yield return new WaitForSeconds(deathDelay);
        
        if (attemptToDesignGameFLow.Instance != null)
        {
            Debug.Log($"[player health] loading death screen: {deathSceneName}");
            attemptToDesignGameFLow.Instance.LoadScene(deathSceneName);
        }
        else
        {
            Debug.LogError("[player health] gameflow system missing - cant load death screen! falling back to restart");
            RestartLevel();
        }
    }

    void RestartLevel()
    {
        UnityEngine.SceneManagement.SceneManager.LoadScene(UnityEngine.SceneManagement.SceneManager.GetActiveScene().name);
    }

    IEnumerator FlashSprite()
    {
        if (spriteRenderer == null) yield break;
        spriteRenderer.color = Color.red; //change to purple for aesthetic later each level
        yield return new WaitForSeconds(flashdurationDisplay);
        spriteRenderer.color = originalColor;

        //short time to go between red state back to normal state for flashing between getting attacked
    }

    //FINALLY I CAN DO THE PUBLIC GETTERS NOW YAAAAAAYYYY 

    public int getHP() => currentHP;
    public int getMaxHP() => maxHP;
    public float getHPperecent() => (float)currentHP / maxHP;
    public bool IsAlive() => !isDead; //fixed: was checking opposite condition before
    public bool IsDead() => isDead;
    
    ///
    ///
    ///
    /// debug methods for testing death screen
    ///
    ///
    ///
    [ContextMenu("Kill dwayne instantly")]
    public void InstantDeath()
    {
        currentHP = 0;
        Dead();
        Debug.Log("[player health] instant death triggered via debug menu");
    }
    
    [ContextMenu("Heal to full")]
    public void HealFull()
    {
        if (isDead) return;
        currentHP = maxHP;
        Debug.Log("[player health] healed to full hp");
    }
}
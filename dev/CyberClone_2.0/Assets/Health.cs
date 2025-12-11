using UnityEngine;
using UnityEngine.Events;

/// <summary>
/// Generic health system component for 2D isometric game entities
/// Handles damage, healing, and death with event callbacks
/// </summary>
public class Health : MonoBehaviour
{
    [Header("Health Configuration")]
    [SerializeField]
    [Tooltip("Maximum health points")]
    private int maxHealth = 100;
    
    [SerializeField]
    [Tooltip("Current health points")]
    private int currentHealth;
    
    [SerializeField]
    [Tooltip("Is this entity invulnerable to damage?")]
    private bool isInvulnerable = false;
    
    [Header("Damage Response")]
    [SerializeField]
    [Tooltip("Duration of invulnerability after taking damage (prevents spam)")]
    private float invulnerabilityDuration = 0.5f;
    
    [SerializeField]
    [Tooltip("Flash sprite when damaged")]
    private bool enableDamageFlash = true;
    
    [SerializeField]
    [Tooltip("Color to flash when damaged")]
    private Color damageFlashColor = new Color(1f, 0.3f, 0.3f, 1f);
    
    [SerializeField]
    [Tooltip("Duration of damage flash effect")]
    private float flashDuration = 0.1f;
    
    [Header("Death Behavior")]
    [SerializeField]
    [Tooltip("Destroy GameObject on death")]
    private bool destroyOnDeath = true;
    
    [SerializeField]
    [Tooltip("Delay before destroying GameObject")]
    private float deathDelay = 0f;
    
    [Header("Audio (Optional)")]
    [SerializeField]
    private AudioClip damageSound;
    
    [SerializeField]
    private AudioClip deathSound;
    
    [SerializeField]
    private AudioClip healSound;
    
    [Header("Events")]
    [Tooltip("Called when entity takes damage. Passes damage amount.")]
    public UnityEvent<int> OnDamage;
    
    [Tooltip("Called when entity is healed. Passes heal amount.")]
    public UnityEvent<int> OnHeal;
    
    [Tooltip("Called when health changes. Passes current health and max health.")]
    public UnityEvent<int, int> OnHealthChanged;
    
    [Tooltip("Called when entity dies.")]
    public UnityEvent OnDeath;
    
    // Component references
    private SpriteRenderer spriteRenderer;
    private AudioSource audioSource;
    
    // State tracking
    private bool isDead = false;
    private float invulnerabilityTimer = 0f;
    private Color originalColor;
    
    /// <summary>
    /// Properties for external access
    /// </summary>
    public int CurrentHealth => currentHealth;
    public int MaxHealth => maxHealth;
    public float HealthPercentage => (float)currentHealth / maxHealth;
    public bool IsDead => isDead;
    public bool IsInvulnerable => isInvulnerable || invulnerabilityTimer > 0f;
    
    void Awake()
    {
        // Initialize health to max
        currentHealth = maxHealth;
        
        // Cache components
        spriteRenderer = GetComponent<SpriteRenderer>();
        audioSource = GetComponent<AudioSource>();
        
        if (spriteRenderer != null)
        {
            originalColor = spriteRenderer.color;
        }
    }
    
    void Update()
    {
        // Update invulnerability timer
        if (invulnerabilityTimer > 0f)
        {
            invulnerabilityTimer -= Time.deltaTime;
            
            // Flash effect during invulnerability
            if (enableDamageFlash && spriteRenderer != null && invulnerabilityTimer > 0f)
            {
                float flashFrequency = 4f; // Flashes per second
                bool shouldFlash = Mathf.Sin(invulnerabilityTimer * flashFrequency * Mathf.PI) > 0;
                spriteRenderer.enabled = shouldFlash;
            }
        }
        else if (spriteRenderer != null && !spriteRenderer.enabled)
        {
            // Restore sprite visibility
            spriteRenderer.enabled = true;
        }
    }
    
    /// <summary>
    /// Apply damage to this entity
    /// </summary>
    /// <param name="damage">Amount of damage to apply</param>
    public void TakeDamage(int damage)
    {
        // Validation checks
        if (isDead || IsInvulnerable || damage <= 0) return;
        
        // Apply damage
        int previousHealth = currentHealth;
        currentHealth = Mathf.Max(0, currentHealth - damage);
        
        // Trigger events
        OnDamage?.Invoke(damage);
        OnHealthChanged?.Invoke(currentHealth, maxHealth);
        
        // Visual feedback
        if (enableDamageFlash)
        {
            StartCoroutine(DamageFlashEffect());
        }
        
        // Audio feedback
        PlaySound(damageSound);
        
        // Start invulnerability
        invulnerabilityTimer = invulnerabilityDuration;
        
        // Check for death
        if (currentHealth <= 0)
        {
            Die();
        }
        
        Debug.Log($"{gameObject.name} took {damage} damage. Health: {currentHealth}/{maxHealth}");
    }
    
    /// <summary>
    /// Heal this entity
    /// </summary>
    /// <param name="healAmount">Amount of health to restore</param>
    public void Heal(int healAmount)
    {
        if (isDead || healAmount <= 0) return;
        
        int previousHealth = currentHealth;
        currentHealth = Mathf.Min(maxHealth, currentHealth + healAmount);
        int actualHealAmount = currentHealth - previousHealth;
        
        if (actualHealAmount > 0)
        {
            // Trigger events
            OnHeal?.Invoke(actualHealAmount);
            OnHealthChanged?.Invoke(currentHealth, maxHealth);
            
            // Audio feedback
            PlaySound(healSound);
            
            Debug.Log($"{gameObject.name} healed for {actualHealAmount}. Health: {currentHealth}/{maxHealth}");
        }
    }
    
    /// <summary>
    /// Set health to a specific value
    /// </summary>
    public void SetHealth(int newHealth)
    {
        currentHealth = Mathf.Clamp(newHealth, 0, maxHealth);
        OnHealthChanged?.Invoke(currentHealth, maxHealth);
        
        if (currentHealth <= 0 && !isDead)
        {
            Die();
        }
    }
    
    /// <summary>
    /// Modify maximum health
    /// </summary>
    public void SetMaxHealth(int newMaxHealth, bool healToMax = false)
    {
        maxHealth = Mathf.Max(1, newMaxHealth);
        
        if (healToMax)
        {
            currentHealth = maxHealth;
        }
        else
        {
            currentHealth = Mathf.Min(currentHealth, maxHealth);
        }
        
        OnHealthChanged?.Invoke(currentHealth, maxHealth);
    }
    
    /// <summary>
    /// Handle death
    /// </summary>
    void Die()
    {
        if (isDead) return;
        
        isDead = true;
        
        // Trigger death event
        OnDeath?.Invoke();
        
        // Play death sound
        PlaySound(deathSound);
        
        // Log death
        Debug.Log($"{gameObject.name} died!");
        
        // Handle destruction
        if (destroyOnDeath)
        {
            if (deathDelay > 0f)
            {
                Destroy(gameObject, deathDelay);
            }
            else
            {
                Destroy(gameObject);
            }
        }
        
        // Disable entity components (optional)
        DisableEntity();
    }
    
    /// <summary>
    /// Disable entity on death
    /// </summary>
    void DisableEntity()
    {
        // Disable movement
        var movement = GetComponent<IsometricCharacterController1>();
        if (movement != null) movement.enabled = false;
        
        var enemyAI = GetComponent<Isometricenemyai>();
        if (enemyAI != null) enemyAI.enabled = false;
        
        // Disable collisions
        var collider = GetComponent<Collider2D>();
        if (collider != null) collider.enabled = false;
        
        // Stop physics
        var rb = GetComponent<Rigidbody2D>();
        if (rb != null)
        {
            rb.linearVelocity = Vector2.zero;
            rb.simulated = false;
        }
    }
    
    /// <summary>
    /// Visual damage feedback coroutine
    /// </summary>
    System.Collections.IEnumerator DamageFlashEffect()
    {
        if (spriteRenderer == null) yield break;
        
        // Flash to damage color
        spriteRenderer.color = damageFlashColor;
        
        yield return new WaitForSeconds(flashDuration);
        
        // Return to original color
        spriteRenderer.color = originalColor;
    }
    
    /// <summary>
    /// Play audio clip
    /// </summary>
    void PlaySound(AudioClip clip)
    {
        if (clip == null) return;
        
        if (audioSource != null)
        {
            audioSource.PlayOneShot(clip);
        }
        else
        {
            AudioSource.PlayClipAtPoint(clip, transform.position);
        }
    }
    
    /// <summary>
    /// Revive entity (useful for respawn systems)
    /// </summary>
    public void Revive(int healthAmount = -1)
    {
        isDead = false;
        
        // Restore health
        currentHealth = healthAmount > 0 ? Mathf.Min(healthAmount, maxHealth) : maxHealth;
        
        // Re-enable components
        var movement = GetComponent<IsometricCharacterController1>();
        if (movement != null) movement.enabled = true;
        
        var enemyAI = GetComponent<Isometricenemyai>();
        if (enemyAI != null) enemyAI.enabled = true;
        
        var collider = GetComponent<Collider2D>();
        if (collider != null) collider.enabled = true;
        
        var rb = GetComponent<Rigidbody2D>();
        if (rb != null) rb.simulated = true;
        
        // Reset sprite
        if (spriteRenderer != null)
        {
            spriteRenderer.enabled = true;
            spriteRenderer.color = originalColor;
        }
        
        // Trigger events
        OnHealthChanged?.Invoke(currentHealth, maxHealth);
    }
    
    // Editor visualization
    #if UNITY_EDITOR
    void OnValidate()
    {
        // Clamp values in editor
        maxHealth = Mathf.Max(1, maxHealth);
        currentHealth = Mathf.Clamp(currentHealth, 0, maxHealth);
    }
    #endif
}

/*
HEALTH SYSTEM TECHNICAL NOTES:

1. DAMAGE SYSTEM:
   - TakeDamage() validates input and applies damage
   - Invulnerability frames prevent damage spam
   - Visual flash feedback for damage indication
   - Death triggers when health reaches zero

2. EVENTS SYSTEM:
   - UnityEvents for UI integration
   - OnDamage: Hook damage VFX/SFX
   - OnHealthChanged: Update health bars
   - OnDeath: Trigger game over, respawn, drops

3. INVULNERABILITY:
   - Temporary immunity after damage
   - Flashing sprite effect during invulnerable period
   - Prevents instant death from multiple hits

4. DEATH HANDLING:
   - Disables movement and AI components
   - Optional GameObject destruction
   - Death delay for animations
   - Revive() method for respawn systems

5. USAGE:
   Player Setup:
   - Add Health component
   - Set maxHealth (e.g., 100)
   - Configure damage flash color
   - Hook OnDeath to game over system
   
   Enemy Setup:
   - Add Health component  
   - Set appropriate maxHealth
   - Enable destroyOnDeath
   - Add death VFX/drops to OnDeath event

6. INTEGRATION:
   - Works with both player and enemy scripts
   - SendMessage fallback in enemy AI still works
   - Public methods for external damage sources

7. EXTENDING:
   - Add armor/defense calculations
   - Implement damage types (fire, ice, etc)
   - Create health pickups that call Heal()
   - Add regeneration over time
*/
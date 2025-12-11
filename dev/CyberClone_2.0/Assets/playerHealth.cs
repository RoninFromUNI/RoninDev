
using UnityEngine;

/// <summary> 
/// 
/// this is gonna be my simply designed health system for the playable character 
/// woth player tag 'player' 
/// Riyaz dont be dumb and make sure you associate this with the same gameobject as isometric character controller 1 
/// 
/// 
/// 
public class playerHealth : MonoBehaviour
{
    [Header("health adjustments")]
    [SerializeField]
    private int maxHP = 100;

    [SerializeField]
    private int currentHP;

    [Header("adjust state of invincibility")]
    [SerializeField]
    [Tooltip("The character needs a state where he doesnt get attacked in repetition, gives a space before attack")]
    private float invincibilityStateDuration = 0.5f;
    private float lastdmgTIme = -999f;

    [Header("Sprite flash when damaged")]
    [SerializeField]
    [Tooltip("Sprite flash when hp dmg occurs")]
    private bool flashDmg = true;

    [SerializeField]
    private float flashdurationDisplay = 0.12f;

    private SpriteRenderer spriteRenderer;
    private Color originalColor;

    void Start()
    {
        currentHP = maxHP;
        spriteRenderer = GetComponent<SpriteRenderer>();

        if (spriteRenderer != null)
        {
            originalColor = spriteRenderer.color;
        }
    }

    public void TakeDamage(int damage)
    {
        if (Time.time < lastdmgTIme + invincibilityStateDuration)
        {
            return;  //edit if only the invincible state is a bit buggy in testing
        }

        lastdmgTIme = Time.time;
        currentHP -= damage;

        Debug.Log($"dwayne  took some mighty damange! {damage} Health is now {currentHP}/{maxHP}");



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

    public void Heal(int amount)
    {
        currentHP = Mathf.Min(currentHP + amount, maxHP);
        Debug.Log($"dwayne has gained health back to {amount}!: Hp = {currentHP}/{maxHP}");
    }

    void Dead()
    {
        Debug.Log("YOu have been flatlined!");

        //gonna come up with some death logic here like a death animation, disabling controls and movement and shwoing a game over scene here
        var controller = GetComponent<IsometricCharacterController1>();
        if (controller != null)
        {
            controller.StopMovement();
            controller.enabled = false;
        }

        // gonna add a estart level if a button is pressed but for now its automatic


    }

    //

    void RestartLevel()
    {
        UnityEngine.SceneManagement.SceneManager.LoadScene(UnityEngine.SceneManagement.SceneManager.GetActiveScene().name);
    }

    System.Collections.IEnumerator FlashSprite()
    {
        if (spriteRenderer == null) yield break;
        spriteRenderer.color = Color.red; //change to purple for aesthetic later each level
        yield return new WaitForSeconds(flashdurationDisplay);
        spriteRenderer.color = originalColor;

        //short time to go between red state back to normal state for flashing between getting attacked
    }

    //FINALLY I CAN DO TH EPUBLIC GETTERS NOW YAAAAAAYYYY 

    public int getHP() => currentHP;
    public int getMaxHP() => maxHP;
    public float getHPperecent() => (float)currentHP / maxHP;
    public bool IsAlive() => currentHP > 0 && currentHP == 0; //need it to be both for immediate death
}
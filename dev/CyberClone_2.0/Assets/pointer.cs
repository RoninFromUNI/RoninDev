using UnityEngine;

/// <summary>
/// so basically i need a pointer that rotates around dwayne to show
/// where im aiming for click based attacks
/// 
/// gotta calculate the angle from character position to mouse cursor
/// accounting for isometric camera projection
/// kinda like hades or diablo style targeting
/// </summary>

public class pointer : MonoBehaviour
{
    [Header("POINTER | CONFIGURATION")]
    [SerializeField]
    [Tooltip("how far from dwayne the pointer sits")]
    private float pointerDistance = 0.07f;
    
    [SerializeField]
    [Tooltip("rotation smoothing speed, 0 = instant snap, higher = smoother follow")]
    private float rotationSmoothing = 15f;
    
    [SerializeField]
    [Tooltip("toggle pointer visibility on/off")]
    private bool showPointer = true;
    ///
    ///
    ///
    [Header("POINTER | ISOMETRIC SETTINGS")]
    [SerializeField]
    [Tooltip("use raycast to get mouse world position instead of ScreenToWorldPoint, better for isometric")]
    private bool useRaycastForMouse = true;
    
    [SerializeField]
    [Tooltip("layermask for ground plane raycast")]
    private LayerMask groundLayer;
    ///
    ///
    ///
    [Header("POINTER | VISUAL SETTINGS")]
    [SerializeField]
    [Tooltip("the sprite renderer for my arrow")]
    private SpriteRenderer pointerRenderer;
    
    [SerializeField]
    [Tooltip("color of the pointer, thinking red for now")]
    private Color pointerColor = Color.red;
    
    [SerializeField]
    [Tooltip("transparency level, 0 = invisible, 1 = solid")]
    private float pointerAlpha = 0.7f;
    
    /// <summary>
    /// 
    /// 
    /// private variables to track state and references
    /// 
    /// 
    /// </summary>
    private Camera mainCamera;
    private Transform characterTransform;
    private Plane groundPlane;
    
    void Start()
    {
        InitializePointer();
    }
    
    void Update()
    {
        if (showPointer)
        {
            UpdatePointerRotation();
        }
    }
    
    /// <summary>
    /// setting up all the component references and visual stuff
    /// auto finding camera and parent transform
    /// creating ground plane for isometric mouse projection
    /// </summary>
    private void InitializePointer()
    {
        mainCamera = Camera.main;
        
        if (mainCamera == null)
        {
            Debug.LogError("[pointer] no main camera found, gotta tag the camera as MainCamera");
            return;
        }
        else
        {
            Debug.Log($"[pointer] camera found | orthographic: {mainCamera.orthographic}");
        }
        
        //getting the parent transform which should be dwayne
        characterTransform = transform.parent;
        
        if (characterTransform == null)
        {
            Debug.LogError("[pointer] AttackPointer needs to be a child of the character gameobject or it won't work");
            return;
        }
        else
        {
            Debug.Log($"[pointer] character transform found: {characterTransform.name}");
        }
        
        //create ground plane at character's Y position for isometric projection
        groundPlane = new Plane(Vector3.forward, new Vector3(0, 0, characterTransform.position.z));
        
        //auto find sprite renderer if i forgot to assign it
        if (pointerRenderer == null)
        {
            pointerRenderer = GetComponent<SpriteRenderer>();
        }
        
        //setting up the color with transparency
        if (pointerRenderer != null)
        {
            Color colorWithAlpha = pointerColor;
            colorWithAlpha.a = pointerAlpha;
            pointerRenderer.color = colorWithAlpha;
            Debug.Log("[pointer] sprite renderer configured");
        }
        else
        {
            Debug.LogWarning("[pointer] no sprite renderer found - pointer won't be visible");
        }
        
        //position the pointer at the distance i set from character center
        transform.localPosition = Vector3.right * pointerDistance;
        
        Debug.Log($"[pointer] initialization complete | distance: {pointerDistance} | smoothing: {rotationSmoothing}");
    }
    
    /// <summary>
    /// this calculates the angle from dwayne to wherever my mouse is
    /// for isometric games, need to project mouse ray onto the ground plane
    /// then calculate angle in 2D space
    /// 
    /// had to learn about Atan2 for this - it returns the arctangent angle
    /// and accounts for all 4 quadrants unlike normal Atan
    /// </summary>
    private void UpdatePointerRotation()
    {
        if (mainCamera == null || characterTransform == null)
        {
            Debug.LogWarning("[pointer] missing camera or character transform - cant update rotation");
            return;
        }
        
        Vector3 mouseWorldPos = GetMouseWorldPosition();
        
        //DEBUG: log every 60 frames
        if (Time.frameCount % 60 == 0)
        {
            Debug.Log($"[pointer] mouse world pos: {mouseWorldPos} | character pos: {characterTransform.position}");
        }
        
        //calculate the direction vector from dwayne to mouse cursor in 2D
        Vector2 directionToMouse = new Vector2(
            mouseWorldPos.x - characterTransform.position.x,
            mouseWorldPos.y - characterTransform.position.y
        );
        
        //Atan2 calculates angle in radians, multiply by Rad2Deg to convert to degrees
        //Atan2(y, x) gives angle where 0° = right, 90° = up, etc
        float angleInDegrees = Mathf.Atan2(directionToMouse.y, directionToMouse.x) * Mathf.Rad2Deg;
        
        //DEBUG: log angle calculation
        if (Time.frameCount % 60 == 0)
        {
            Debug.Log($"[pointer] direction: {directionToMouse} | angle: {angleInDegrees}°");
        }
        
        //apply rotation smoothing if enabled, otherwise instant snap
        if (rotationSmoothing > 0f)
        {
            //lerp the rotation smoothly toward target angle
            float currentRotation = transform.eulerAngles.z;
            float smoothedAngle = Mathf.LerpAngle(currentRotation, angleInDegrees, rotationSmoothing * Time.deltaTime);
            transform.rotation = Quaternion.Euler(0f, 0f, smoothedAngle);
        }
        else
        {
            //instant rotation with no smoothing
            transform.rotation = Quaternion.Euler(0f, 0f, angleInDegrees);
        }
    }
    
    /// <summary>
    /// 
    /// get mouse world position accounting for isometric camera
    /// uses plane raycast for accurate positioning
    /// 
    /// </summary>
    private Vector3 GetMouseWorldPosition()
    {
        Vector3 mousePos = Input.mousePosition;
        
        if (useRaycastForMouse && groundLayer != 0)
        {
            //raycast method - more accurate for isometric with ground plane
            Ray ray = mainCamera.ScreenPointToRay(mousePos);
            RaycastHit2D hit = Physics2D.GetRayIntersection(ray, Mathf.Infinity, groundLayer);
            
            if (hit.collider != null)
            {
                return hit.point;
            }
        }
        
        //fallback to plane intersection method
        Ray cameraRay = mainCamera.ScreenPointToRay(mousePos);
        float rayDistance;
        
        if (groundPlane.Raycast(cameraRay, out rayDistance))
        {
            Vector3 worldPoint = cameraRay.GetPoint(rayDistance);
            worldPoint.z = characterTransform.position.z; //keep same Z as character
            return worldPoint;
        }
        
        //final fallback to ScreenToWorldPoint
        Vector3 screenPoint = mousePos;
        screenPoint.z = mainCamera.WorldToScreenPoint(characterTransform.position).z;
        Vector3 worldPos = mainCamera.ScreenToWorldPoint(screenPoint);
        worldPos.z = characterTransform.position.z;
        return worldPos;
    }
    
    /// <summary>
    /// public method to toggle pointer on/off
    /// might be useful during cutscenes or menus
    /// </summary>
    public void SetPointerVisibility(bool visible)
    {
        showPointer = visible;
        
        if (pointerRenderer != null)
        {
            pointerRenderer.enabled = visible;
        }
        
        Debug.Log($"[pointer] visibility set to: {visible}");
    }
    
    /// <summary>
    /// returns the normalized direction vector from character to mouse
    /// this is what i'll use for attack direction when i implement clicking
    /// </summary>
    public Vector2 GetPointerDirection()
    {
        if (mainCamera == null || characterTransform == null) return Vector2.right;
        
        Vector3 mouseWorldPos = GetMouseWorldPosition();
        
        Vector2 direction = new Vector2(
            mouseWorldPos.x - characterTransform.position.x,
            mouseWorldPos.y - characterTransform.position.y
        ).normalized;
        
        return direction;
    }
    
    /// <summary>
    /// returns current pointer angle in degrees
    /// 0° = right, 90° = up, 180° = left, 270° = down
    /// </summary>
    public float GetPointerAngle()
    {
        return transform.eulerAngles.z;
    }
    
    /// <summary>
    /// 
    /// 
    /// gizmo visualization for scene view debugging
    /// draws a line from dwayne to the pointer position
    /// and shows mouse world position
    /// 
    /// 
    /// </summary>
    private void OnDrawGizmosSelected()
    {
        if (characterTransform == null)
        {
            characterTransform = transform.parent;
        }
        
        if (characterTransform != null)
        {
            //cyan line showing pointer direction
            Gizmos.color = Color.cyan;
            Gizmos.DrawLine(characterTransform.position, transform.position);
            Gizmos.DrawWireSphere(transform.position, 0.02f);
            
            //yellow sphere at mouse world position
            if (Application.isPlaying && mainCamera != null)
            {
                Vector3 mouseWorld = GetMouseWorldPosition();
                Gizmos.color = Color.yellow;
                Gizmos.DrawWireSphere(mouseWorld, 0.05f);
                Gizmos.DrawLine(characterTransform.position, mouseWorld);
            }
        }
    }
}